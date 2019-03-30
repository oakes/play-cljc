(ns play-cljc.math
  (:require #?@(:clj  [[play-cljc.macros-java :refer [math]]
                       [mikera.vectorz.matrix :as mat]]
                :cljs [[play-cljc.macros-js :refer-macros [math]]])))

(defn vector->array [v]
  (#?(:clj float-array :cljs clj->js) v))

(defn vector->2d-array [v]
  (#?(:clj to-array-2d :cljs clj->js) v))

(defn identity-matrix [size]
  #?(:clj
     (vec (mat/as-vector (mat/identity-matrix size)))

     :cljs
     (vec (for [row (range size)
                col (range size)]
            (if (= row col) 1 0)))))

(defn multiply-matrices [size m1 m2]
  #?(:clj
     (let [m1 (mat/matrix (partition size m1))
           m2 (mat/matrix (partition size (or m2 (identity-matrix size))))]
       (vec (mat/as-vector (mat/* m1 m2))))

     :cljs
     (let [m1 (mapv vec (partition size m1))
           m2 (mapv vec (partition size (or m2 (identity-matrix size))))
           result (for [i (range size)
                        j (range size)]
                    (reduce
                     (fn [sum k]
                       (+ sum (* (get-in m1 [i k])
                                 (get-in m2 [k j]))))
                     0
                     (range size)))]
       (vec result))))

(defn inverse-matrix [size m]
  #?(:clj
     (let [mc (mat/matrix (partition size m))]
       (vec (mat/as-vector (mat/inverse mc))))

     :cljs
     (let [mc (mapv vec (partition size m))
           mi (mapv vec (for [i (range size)]
                         (for [j (range size)]
                           (if (= i j) 1 0))))
           mc (vector->2d-array mc)
           mi (vector->2d-array mi)]
       (dotimes [i size]
         (when (= 0 (aget mc i i))
           (loop [r (range (+ i 1) size)]
             (when-let [ii (first r)]
               (if (not= 0 (aget mc ii i))
                 (dotimes [j size]
                   (let [e (aget mc i j)]
                     (aset mc i j (aget mc ii j))
                     (aset mc ii j e))
                   (let [e (aget mi i j)]
                     (aset mi i j (aget mi ii j))
                     (aset mi ii j e)))
                 (recur (rest r))))))
         (let [e (aget mc i i)]
           (when (= 0 e)
             (throw (ex-info "Not invertable" {})))
           (dotimes [j size]
             (aset mc i j (/ (aget mc i j) e))
             (aset mi i j (/ (aget mi i j) e))))
         (dotimes [ii size]
           (when (not= i ii)
             (let [e (aget mc ii i)]
               (dotimes [j size]
                 (aset mc ii j
                   (- (aget mc ii j)
                     (* e (aget mc i j))))
                 (aset mi ii j
                   (- (aget mi ii j)
                     (* e (aget mi i j)))))))))
       (->> mi seq (apply concat) vec))))

(defn deg->rad [d]
  (-> d (* (math PI)) (/ 180)))

(defn transform-vector [m v]
  (let [^floats dst (vector->array v)]
    (dotimes [i 4]
      (#?(:clj aset-float :cljs aset) dst i 0.0)
      (dotimes [j 4]
        (#?(:clj aset-float :cljs aset) dst i
          (+ (aget dst i)
             (* (nth v j)
                (nth m (-> j (* 4) (+ i))))))))
    (vec dst)))

(defn- cross [a b]
  [(- (* (nth a 1) (nth b 2))
      (* (nth a 2) (nth b 1)))
   (- (* (nth a 2) (nth b 0))
      (* (nth a 0) (nth b 2)))
   (- (* (nth a 0) (nth b 1))
      (* (nth a 1) (nth b 0)))])

(defn- subtract-vectors [a b]
  [(- (nth a 0) (nth b 0))
   (- (nth a 1) (nth b 1))
   (- (nth a 2) (nth b 2))])

(defn- normalize [v]
  (let [length (math sqrt
                 (+ (* (nth v 0) (nth v 0))
                    (* (nth v 1) (nth v 1))
                    (* (nth v 2) (nth v 2))))]
    (if (> length 0.00001)
      [(/ (nth v 0) length)
       (/ (nth v 1) length)
       (/ (nth v 2) length)]
      [0 0 0])))

;; two-d

(defn translation-matrix [tx ty]
  [1 0 0
   0 1 0
   tx ty 1])

(defn rotation-matrix [angle-in-radians]
  (let [c (math cos angle-in-radians)
        s (math sin angle-in-radians)]
    [c (- s) 0
     s c 0
     0 0 1]))

(defn scaling-matrix [sx sy]
  [sx 0 0
   0 sy 0
   0 0 1])

(defn projection-matrix [width height]
  [(/ 2 width) 0 0
   0 (/ -2 height) 0
   -1 1 1])

(defn look-at-matrix [target up]
  (let [z-axis (normalize target)
        x-axis (normalize (cross up z-axis))
        y-axis (normalize (cross z-axis x-axis))]
    [(nth x-axis 0) (nth x-axis 1) (nth x-axis 2)
     (nth y-axis 0) (nth y-axis 1) (nth y-axis 2)
     (nth z-axis 0) (nth z-axis 1) (nth z-axis 2)]))

;; three-d

(defn translation-matrix-3d [tx ty tz]
  [1,  0,  0,  0,
   0,  1,  0,  0,
   0,  0,  1,  0,
   tx, ty, tz, 1,])

(defn x-rotation-matrix-3d [angle-in-radians]
  (let [c (math cos angle-in-radians)
        s (math sin angle-in-radians)]
    [1, 0, 0, 0,
     0, c, s, 0,
     0, (- s), c, 0,
     0, 0, 0, 1]))

(defn y-rotation-matrix-3d [angle-in-radians]
  (let [c (math cos angle-in-radians)
        s (math sin angle-in-radians)]
    [c, 0, (- s), 0,
     0, 1, 0, 0,
     s, 0, c, 0,
     0, 0, 0, 1,]))

(defn z-rotation-matrix-3d [angle-in-radians]
  (let [c (math cos angle-in-radians)
        s (math sin angle-in-radians)]
    [c, s, 0, 0,
     (- s), c, 0, 0,
     0, 0, 1, 0,
     0, 0, 0, 1,]))

(defn scaling-matrix-3d [sx sy sz]
  [sx, 0,  0,  0,
   0, sy,  0,  0,
   0,  0, sz,  0,
   0,  0,  0,  1,])

(defn ortho-matrix-3d [left right bottom top near far]
  (let [width (- right left)
        height (- top bottom)
        depth (- near far)]
    [(/ 2 width) 0 0 0
     0 (/ 2 height) 0 0
     0 0 (/ 2 depth) 0
     (/ (+ left right)
        (- left right))
     (/ (+ bottom top)
        (- bottom top))
     (/ (+ near far)
        (- near far))
     1]))

(defn perspective-matrix-3d [field-of-view aspect near far]
  (let [f (math tan (- (* (math PI) 0.5)
                       (* field-of-view 0.5)))
        range-inv (/ 1 (- near far))]
    [(/ f aspect) 0 0 0
     0 f 0 0
     0 0 (* (+ near far) range-inv) -1
     0 0 (* near far range-inv 2) 0]))

(defn transpose-matrix-3d [m]
  [(nth m 0) (nth m 4) (nth m 8) (nth m 12)
   (nth m 1) (nth m 5) (nth m 9) (nth m 13)
   (nth m 2) (nth m 6) (nth m 10) (nth m 14)
   (nth m 3) (nth m 7) (nth m 11) (nth m 15)])

(defn look-at-matrix-3d [camera-pos target up]
  (let [z-axis (normalize (subtract-vectors camera-pos target))
        x-axis (normalize (cross up z-axis))
        y-axis (normalize (cross z-axis x-axis))]
    [(nth x-axis 0) (nth x-axis 1) (nth x-axis 2) 0
     (nth y-axis 0) (nth y-axis 1) (nth y-axis 2) 0
     (nth z-axis 0) (nth z-axis 1) (nth z-axis 2) 0
     (nth camera-pos 0) (nth camera-pos 1) (nth camera-pos 2) 1]))

