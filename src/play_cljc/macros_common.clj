(ns play-cljc.macros-common)

(defmacro mul-mat-fn [size]
  (let [params (repeatedly (* 2 size size)
                           #(with-meta (gensym) {:tag 'double}))
        [m1 m2] (map vec (partition (* size size) params))]
    `(fn ~[m1 m2]
       ~(vec (for [i (range size) j (range size)]
               (cons `+ (for [k (range size)]
                          `(* ~(nth m1 (+ (* i size) k))
                              ~(nth m2 (+ (* k size) j))))))))))

(defn- sub-det-binds [rows cols sub-det-syms]
  (when (next rows)
    (let [m-rows (map #(remove (partial = %) rows) rows)
          binds (mapcat #(sub-det-binds % (rest cols) sub-det-syms) m-rows)
          first-col (map #(sub-det-syms `(~%) `(~(first cols))) rows)
          minors (map #(sub-det-syms % (rest cols)) m-rows)
          [fform & forms] (map #(list `* %1 %2) first-col minors)]
      (conj (vec binds)
            (sub-det-syms rows cols)
            `(-> ~fform ~@(map #(list %1 %2) (cycle [`- `+]) forms))))))

(defn- mat-inv-fn-binds [size sub-det-syms]
  (transduce (comp
              (map #(map (fn [k] (remove (partial = k) (range size))) %))
              (mapcat (fn [[rows cols]] (sub-det-binds rows cols sub-det-syms)))
              (distinct))
             conj [] (into '([-1 -1]) (for [i (range size) j (range size)]
                                        [i j]))))

(defmacro inv-mat-fn [size]
  (let [sub-det-syms (memoize (fn [rows cols]
                                (with-meta (gensym) (when-not (next rows)
                                                      {:tag 'double}))))
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
