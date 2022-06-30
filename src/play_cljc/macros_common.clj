(ns play-cljc.macros-common)

(defmacro mul-mat-fn
  "Produces optimized code for a matrix multiplication function
  for matrices of the given size. The resulting code has no loops
  or branches."
  [size]
  (let [params (repeatedly (* 2 size size)
                           #(with-meta (gensym) {:tag 'double}))
        [m1 m2] (map vec (partition (* size size) params))]
    `(fn ~[m1 m2]
       ~(vec (for [i (range size) j (range size)]
               (cons `+ (for [k (range size)]
                          `(* ~(nth m1 (+ (* i size) k))
                              ~(nth m2 (+ (* k size) j))))))))))

;; Inverse matrix:
;;
;; The functions `sub-det-binds`, `mat-inv-fn-binds`, and the macro
;; `mat-inv-fn` all make use of a function `sub-det-syms` that takes
;; a list of row indices, a list of column indices, and returns a
;; symbol representing the determinant of the corresponding sub-matrix.
;; As a special case, you get symbols for the entries of the matrix by
;; passing in singleton lists for the rows and columns. This function
;; is created by the `mat-inv-fn` macro and passed into `sub-det-binds`
;; and `mat-inv-fn-binds`.

(defn- sub-det-binds
  "Returns a vector of local bindings, one for the determinant
  of the given sub-matrix and some for all sub-determinants used to
  compute it. Specifically, it generates bindings for the minors of
  the sub-matrix along its first column, which are used in cofactor
  expansion. The `rows` and `cols` parameters are lists of indices
  specifying the rows and columns that are part of the sub-matrix."
  [rows cols sub-det-syms]
  (when (next rows) ;; if the sub-matrix is 1x1, then it doesn't need bindings
    (let [;; create row lists and bindings for each minor we need
          m-rows (map #(remove (partial = %) rows) rows)
          m-binds (mapcat #(sub-det-binds % (rest cols) sub-det-syms) m-rows)
          ;; make all the multiplication forms
          first-col (map #(sub-det-syms `(~%) `(~(first cols))) rows)
          minors (map #(sub-det-syms % (rest cols)) m-rows)
          [fform & forms] (map #(list `* %1 %2) first-col minors)]
      (conj (vec m-binds)
            (sub-det-syms rows cols)
            ;; make alternating sum of our multiplication forms
            `(-> ~fform ~@(map #(list %1 %2) (cycle [`- `+]) forms))))))

(defn- mat-inv-fn-binds
  "Returns the local bindings vector to be used in the code
  produced by `inv-mat-fn`. There are bindings for all the minors
  of the input matrix, a binding for the determinant of the input matrix,
  and bindings for all sub-matrix determinants that are used to compute
  the minors and the determinant."
  [size sub-det-syms]
  (transduce (comp
              ;; map [i j] to [rows cols] for the corresponding minor
              (map #(map (fn [k] (remove (partial = k) (range size))) %))
              (mapcat (fn [[rows cols]] (sub-det-binds rows cols sub-det-syms)))
              (distinct))
             conj [] (conj (vec (for [i (range size) j (range size)] [i j]))
                           [-1 -1]))) ;; [-1 -1] corresponds to the determinant

(defmacro inv-mat-fn
  "Produces optimized code for a function that inverts matrices
  of the given size. The resulting code has no loops, branches, or
  recursive calls. The inverse is computed using Cramer's rule and
  the required determinants are computed using recursive cofactor
  expansion along the first column. All intermediate determinants
  are bound to local variables since many of them are used multiple
  times."
  [size]
  (let [sub-det-syms (memoize (fn [rows cols]
                                (with-meta (gensym)
                                  ;; we only tag the matrix entries
                                  (when-not (next rows) {:tag 'double}))))
        m (vec (for [i (range size) j (range size)]
                 (sub-det-syms `(~i) `(~j))))]
    `(fn ~[m]
       (let ~(mat-inv-fn-binds size sub-det-syms)
         ~(vec (for [i (range size) j (range size)]
                 (let [detA (sub-det-syms (range size) (range size))
                       detA_ij (sub-det-syms (remove #(= j %) (range size))
                                             (remove #(= i %) (range size)))]
                   (if (even? (+ i j))
                     `(/ ~detA_ij ~detA)
                     `(- (/ ~detA_ij ~detA))))))))))
