(ns play-cljc.primitives-3d
  (:require #?(:clj  [play-cljc.macros-java :refer [math]]
               :cljs [play-cljc.macros-js :refer-macros [math]])
            [clojure.spec.alpha :as s]))

(s/def ::width number?)
(s/def ::depth number?)
(s/def ::radius number?)
(s/def ::subdivisions-width number?)
(s/def ::subdivisions-depth number?)
(s/def ::subdivisions-axis pos?)
(s/def ::subdivisions-height pos?)
(s/def ::bottom-radius number?)
(s/def ::top-radius number?)
(s/def ::radial-subdivisions #(>= % 3))
(s/def ::vertical-subdivisions #(>= % 1))
(s/def ::body-subdivisions #(>= % 3))
(s/def ::top-cap? boolean?)
(s/def ::bottom-cap? boolean?)
(s/def ::vertical-radius number?)
(s/def ::inner-radius number?)
(s/def ::outer-radius number?)
(s/def ::thickness number?)
(s/def ::subdivisions-down pos?)
(s/def ::start-offset number?)
(s/def ::end-offset number?)
(s/def ::start-angle number?)
(s/def ::end-angle number?)
(s/def ::divisions #(>= % 3))
(s/def ::stacks number?)
(s/def ::stack-power number?)

(s/fdef plane
  :args (s/cat :props (s/keys :opt-un [::width ::depth ::subdivisions-width ::subdivisions-depth])))

(defn plane [{:keys [width depth subdivisions-width subdivisions-depth]
              :or {width 1 depth 1 subdivisions-width 1 subdivisions-depth 1}}]
  (let [num-verts-across (inc subdivisions-width)]
    (-> (fn [m z]
          (reduce
            (fn [m x]
              (let [u (/ x subdivisions-width)
                    v (/ z subdivisions-depth)]
                (-> m
                    (update :positions (fn [positions]
                                         (-> positions
                                             (conj! (- (* width u) (* width 0.5)))
                                             (conj! 0)
                                             (conj! (- (* depth v) (* depth 0.5))))))
                    (update :normals (fn [normals]
                                       (-> normals
                                           (conj! 0)
                                           (conj! 1)
                                           (conj! 0))))
                    (update :texcoords (fn [texcoords]
                                         (-> texcoords
                                             (conj! u)
                                             (conj! v)))))))
            m
            (range (inc subdivisions-width))))
        (reduce
          {:positions (transient [])
           :normals (transient [])
           :texcoords (transient [])}
          (range (inc subdivisions-depth)))
        (assoc :indices
          (reduce
            (fn [indices z]
              (reduce
                (fn [indices x]
                  (-> indices
                      ;; triangle 1
                      (conj! (-> z (* num-verts-across) (+ x)))
                      (conj! (-> z inc (* num-verts-across) (+ x)))
                      (conj! (-> z (* num-verts-across) (+ (inc x))))
                      ;; triangle 2
                      (conj! (-> z inc (* num-verts-across) (+ x)))
                      (conj! (-> z inc (* num-verts-across) (+ (inc x))))
                      (conj! (-> z (* num-verts-across) (+ (inc x))))))
                indices
                (range subdivisions-width)))
            (transient [])
            (range subdivisions-depth)))
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (update :indices persistent!))))

(s/fdef sphere
  :args (s/cat :props (s/keys :req-un [::radius ::subdivisions-axis ::subdivisions-height])))

(defn sphere [{:keys [radius subdivisions-axis subdivisions-height]}]
  (let [start-latitude 0
        end-latitude (math PI)
        start-longitude 0
        end-longitude (* 2 (math PI))
        lat-range (- end-latitude start-latitude)
        long-range (- end-longitude start-longitude)
        num-vertices (* (inc subdivisions-axis) (inc subdivisions-height))
        num-verts-around (inc subdivisions-axis)]
    (-> (fn [m y]
          (reduce
            (fn [m x]
              (let [u (/ x subdivisions-axis)
                    v (/ y subdivisions-height)
                    theta (+ (* long-range u) start-longitude)
                    phi (+ (* lat-range v) start-latitude)
                    sin-theta (math sin theta)
                    cos-theta (math cos theta)
                    sin-phi (math sin phi)
                    cos-phi (math cos phi)
                    ux (* cos-theta sin-phi)
                    uy cos-phi
                    uz (* sin-theta sin-phi)]
                (-> m
                    (update :positions (fn [positions]
                                         (-> positions
                                             (conj! (* radius ux))
                                             (conj! (* radius uy))
                                             (conj! (* radius uz)))))
                    (update :normals (fn [normals]
                                       (-> normals
                                           (conj! ux)
                                           (conj! uy)
                                           (conj! uz))))
                    (update :texcoords (fn [texcoords]
                                         (-> texcoords
                                             (conj! (- 1 u))
                                             (conj! v)))))))
            m
            (range (inc subdivisions-axis))))
        (reduce
          {:positions (transient [])
           :normals (transient [])
           :texcoords (transient [])}
          (range (inc subdivisions-height)))
        (assoc :indices
          (reduce
            (fn [indices y]
              (reduce
                (fn [indices x]
                  (-> indices
                      ;; triangle 1
                      (conj! (-> y (* num-verts-around) (+ x)))
                      (conj! (-> y (* num-verts-around) (+ (inc x))))
                      (conj! (-> y inc (* num-verts-around) (+ x)))
                      ;; triangle 2
                      (conj! (-> y inc (* num-verts-around) (+ x)))
                      (conj! (-> y (* num-verts-around) (+ (inc x))))
                      (conj! (-> y inc (* num-verts-around) (+ (inc x))))))
                indices
                (range subdivisions-axis)))
            (transient [])
            (range subdivisions-height)))
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (update :indices persistent!))))

(def cube-face-indices
  [; right
   [3 7 5 1]
   ; left
   [6 2 0 4]
   [6 7 3 2]
   [0 1 5 4]
   ; front
   [7 6 4 5]
   ; back
   [2 3 1 0]])

(defn cube [{:keys [size] :or {size 1}}]
  (let [k (/ size 2)
        corner-vertices [[(- k) (- k) (- k)]
                         [k (- k) (- k)]
                         [(- k) k (- k)]
                         [k k (- k)]
                         [(- k) (- k) k]
                         [k (- k) k]
                         [(- k) k k]
                         [k k k]]
        face-normals [[1 0 0]
                      [-1 0 0]
                      [0 1 0]
                      [0 -1 0]
                      [0 0 1]
                      [0 0 -1]]
        uv-coords [[1 0]
                   [0 0]
                   [0 1]
                   [1 1]]
        num-vertices (* 6 4)]
    (-> (fn [m f]
          (let [face-indices (nth cube-face-indices f)
                offset (* 4 f)]
            (-> (fn [m v]
                  (-> m
                      (update :positions (fn [positions]
                                           (let [[x y z] (nth corner-vertices (nth face-indices v))]
                                             (-> positions
                                                 (conj! x)
                                                 (conj! y)
                                                 (conj! z)))))
                      (update :normals (fn [normals]
                                         (let [[x y z] (nth face-normals f)]
                                           (-> normals
                                               (conj! x)
                                               (conj! y)
                                               (conj! z)))))
                      (update :texcoords (fn [texcoords]
                                           (let [[u v] (nth uv-coords v)]
                                             (-> texcoords
                                                 (conj! u)
                                                 (conj! v)))))))
                (reduce m (range 4))
                (update :indices (fn [indices]
                                   (-> indices
                                       (conj! (+ offset 0))
                                       (conj! (+ offset 1))
                                       (conj! (+ offset 2))
                                       (conj! (+ offset 0))
                                       (conj! (+ offset 2))
                                       (conj! (+ offset 3))))))))
        (reduce
          {:positions (transient [])
           :normals (transient [])
           :texcoords (transient [])
           :indices (transient [])}
          (range 6))
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (update :indices persistent!))))

(s/fdef cylinder
  :args (s/cat :props (s/keys
                        :req-un [::bottom-radius ::top-radius
                                 ::radial-subdivisions ::vertical-subdivisions]
                        :opt-un [::top-cap? ::bottom-cap?])))

(defn cylinder [{:keys [bottom-radius top-radius height
                        radial-subdivisions vertical-subdivisions
                        top-cap? bottom-cap?]
                 :or {top-cap? true bottom-cap? true}}]
  (let [extra (+ (if top-cap? 2 0) (if bottom-cap? 2 0))
        num-vertices (* (inc radial-subdivisions)
                        (+ vertical-subdivisions 1 extra))
        verts-around-edge (inc radial-subdivisions)
        slant (math atan2 (- bottom-radius top-radius) height)
        cos-slant (math cos slant)
        sin-slant (math sin slant)
        start (if top-cap? -2 0)
        end (+ vertical-subdivisions (if bottom-cap? 2 0))]
    (-> (fn [m yy]
          (let [v (/ yy vertical-subdivisions)
                y (* height v)
                [y v ring-radius]
                (cond
                  (< yy 0)
                  [0
                   1
                   bottom-radius]
                  (> yy vertical-subdivisions)
                  [height
                   1
                   top-radius]
                  :else
                  [y
                   v
                   (+ bottom-radius
                      (* (- top-radius bottom-radius)
                         (/ yy vertical-subdivisions)))])
                [y v ring-radius]
                (if (or (= yy -2)
                        (= yy (+ vertical-subdivisions 2)))
                  [y 0 0]
                  [y v ring-radius])
                y (- y (/ height 2))]
            (reduce
              (fn [m ii]
                (let [sin (math sin (-> ii (* (math PI)) (* 2) (/ radial-subdivisions)))
                      cos (math cos (-> ii (* (math PI)) (* 2) (/ radial-subdivisions)))]
                  (-> m
                      (update :positions (fn [positions]
                                           (-> positions
                                               (conj! (* sin ring-radius))
                                               (conj! y)
                                               (conj! (* cos ring-radius)))))
                      (update :normals (fn [normals]
                                         (-> normals
                                             (conj! (if (or (< yy 0)
                                                            (> yy vertical-subdivisions))
                                                      0
                                                      (* sin cos-slant)))
                                             (conj! (if (< yy 0)
                                                      -1
                                                      (if (> yy vertical-subdivisions)
                                                        1
                                                        sin-slant)))
                                             (conj! (if (or (< yy 0)
                                                            (> yy vertical-subdivisions))
                                                      0
                                                      (* cos cos-slant))))))
                      (update :texcoords (fn [texcoords]
                                           (-> texcoords
                                               (conj! (/ ii radial-subdivisions))
                                               (conj! (- 1 v))))))))
              
              m
              (range verts-around-edge))))
        (reduce
          {:positions (transient [])
           :normals (transient [])
           :texcoords (transient [])}
          (range start (inc end)))
        (assoc :indices
          (reduce
            (fn [indices yy]
              (reduce
                (fn [indices ii]
                  (-> indices
                      ;; triangle 1
                      (conj! (-> verts-around-edge (* (+ yy 0)) (+ 0) (+ ii)))
                      (conj! (-> verts-around-edge (* (+ yy 0)) (+ 1) (+ ii)))
                      (conj! (-> verts-around-edge (* (+ yy 1)) (+ 1) (+ ii)))
                      ;; triangle 2
                      (conj! (-> verts-around-edge (* (+ yy 0)) (+ 0) (+ ii)))
                      (conj! (-> verts-around-edge (* (+ yy 1)) (+ 1) (+ ii)))
                      (conj! (-> verts-around-edge (* (+ yy 1)) (+ 0) (+ ii)))))
                indices
                (range radial-subdivisions)))
            (transient [])
            (range (+ vertical-subdivisions extra))))
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (update :indices persistent!))))

(s/fdef crescent
  :args (s/cat :props (s/keys
                        :req-un [::vertical-radius ::outer-radius ::inner-radius
                                 ::thickness ::subdivisions-down]
                        :opt-un [::start-offset ::end-offset])))

(defn crescent [{:keys [vertical-radius outer-radius inner-radius thickness
                        subdivisions-down start-offset end-offset]
                 :or {start-offset 0 end-offset 1}}]
  (let [subdivisions-thick 2
        offset-range (- end-offset start-offset)
        num-vertices (-> (inc subdivisions-down) (* 2) (* (+ 2 subdivisions-thick)))
        lerp (fn [a b s]
               (-> a (+ (- b a)) (* s)))
        create-arc (fn [m arc-radius x normal-mult normal-add u-mult u-add]
                     (reduce
                       (fn [m z]
                         (let [u-back (/ x (dec subdivisions-thick))
                               v (/ z subdivisions-down)
                               x-back (* (- u-back 0.5) 2)
                               angle (-> (* v offset-range) (+ start-offset) (* (math PI)))
                               s (math sin angle)
                               c (math cos angle)
                               radius (lerp vertical-radius arc-radius s)
                               px (* x-back thickness)
                               py (* c vertical-radius)
                               pz (* s radius)
                               [nx ny nz] (->> [0 s c]
                                               (mapv * normal-mult)
                                               (mapv + normal-add))]
                           (-> m
                               (update :positions (fn [positions]
                                                    (-> positions
                                                        (conj! px)
                                                        (conj! py)
                                                        (conj! pz))))
                               (update :normals (fn [normals]
                                                  (-> normals
                                                      (conj! nx)
                                                      (conj! ny)
                                                      (conj! nz))))
                               (update :texcoords (fn [texcoords]
                                                    (-> texcoords
                                                        (conj! (+ (* u-back u-mult) u-add))
                                                        (conj! v)))))))
                       m
                       (range (inc subdivisions-down))))
        num-vertices-down (inc subdivisions-down)
        create-surface (fn [m left-arc-offset right-arc-offset]
                         (update m :indices
                           (fn [indices]
                             (reduce
                               (fn [indices z]
                                 (-> indices
                                     ;; triangle 1
                                     (conj! (+ left-arc-offset z 0))
                                     (conj! (+ left-arc-offset z 1))
                                     (conj! (+ right-arc-offset z 0))
                                     ;; triangle 2
                                     (conj! (+ left-arc-offset z 1))
                                     (conj! (+ right-arc-offset z 1))
                                     (conj! (+ right-arc-offset z 0))))
                               indices
                               (range subdivisions-down)))))]
    (-> (fn [m x]
          (let [u-back (-> x (/ (dec subdivisions-thick)) (- 0.5) (* 2))]
            (-> m
                (create-arc outer-radius x [1 1 1] [0 0 0] 1 0)
                (create-arc outer-radius x [0 0 0] [u-back 0 0] 0 0)
                (create-arc inner-radius x [1 1 1] [0 0 0] 1 0)
                (create-arc inner-radius x [0 0 0] [u-back 0 0] 0 1))))
        (reduce
          {:positions (transient [])
           :normals (transient [])
           :texcoords (transient [])
           :indices (transient [])}
          (range subdivisions-thick))
        (create-surface (* num-vertices-down 0) (* num-vertices-down 4)) ;; front
        (create-surface (* num-vertices-down 5) (* num-vertices-down 7)) ;; right
        (create-surface (* num-vertices-down 6) (* num-vertices-down 2)) ;; back
        (create-surface (* num-vertices-down 3) (* num-vertices-down 1)) ;; left
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (update :indices persistent!))))

(s/fdef torus
  :args (s/cat :props (s/keys
                        :req-un [::radius ::thickness ::radial-subdivisions ::body-subdivisions]
                        :opt-un [::start-angle ::end-angle])))

(defn torus [{:keys [radius thickness radial-subdivisions body-subdivisions
                     start-angle end-angle]
              :or {start-angle 0 end-angle (* 2 (math PI))}}]
  (let [angle-range (- end-angle start-angle)
        radial-parts (inc radial-subdivisions)
        body-parts (inc body-subdivisions)
        num-vertices (* radial-parts body-parts)]
    (-> (fn [m slice]
          (let [v (/ slice body-subdivisions)
                slice-angle (* v (math PI) 2)
                slice-sin (math sin slice-angle)
                ring-radius (+ radius (* slice-sin thickness))
                ny (math cos slice-angle)
                y (* ny thickness)]
            (reduce
              (fn [m ring]
                (let [u (/ ring radial-subdivisions)
                      ring-angle (+ start-angle (* u angle-range))
                      x-sin (math sin ring-angle)
                      z-cos (math cos ring-angle)
                      x (* x-sin ring-radius)
                      z (* z-cos ring-radius)
                      nx (* x-sin slice-sin)
                      nz (* z-cos slice-sin)]
                  (-> m
                      (update :positions (fn [positions]
                                           (-> positions
                                               (conj! x)
                                               (conj! y)
                                               (conj! z))))
                      (update :normals (fn [normals]
                                         (-> normals
                                             (conj! nx)
                                             (conj! ny)
                                             (conj! nz))))
                      (update :texcoords (fn [texcoords]
                                           (-> texcoords
                                               (conj! u)
                                               (conj! (- 1 v))))))))
              m
              (range radial-parts))))
        (reduce
          {:positions (transient [])
           :normals (transient [])
           :texcoords (transient [])}
          (range body-parts))
        (assoc :indices
          (reduce
            (fn [indices slice]
              (reduce
                (fn [indices ring]
                  (let [next-ring-index (inc ring)
                        next-slice-index (inc slice)]
                    (-> indices
                        ;; triangle 1
                        (conj! (+ (* radial-parts slice) ring))
                        (conj! (+ (* radial-parts next-slice-index) ring))
                        (conj! (+ (* radial-parts slice) next-ring-index))
                        ;; triangle 2
                        (conj! (+ (* radial-parts next-slice-index) ring))
                        (conj! (+ (* radial-parts next-slice-index) next-ring-index))
                        (conj! (+ (* radial-parts slice) next-ring-index)))))
                indices
                (range radial-subdivisions)))
            (transient [])
            (range body-subdivisions)))
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (update :indices persistent!))))

(s/fdef disc
  :args (s/cat :props (s/keys
                        :req-un [::radius ::divisions]
                        :opt-un [::stacks ::inner-radius ::stack-power])))

(defn disc [{:keys [radius divisions stacks inner-radius stack-power]
             :or {stacks 1 inner-radius 0 stack-power 1}}]
  (let [num-vertices (* (inc divisions) (inc stacks))
        radius-span (- radius inner-radius)
        points-per-stack (inc divisions)]
    (-> (fn [m stack]
          (let [stack-radius (+ inner-radius
                                (* radius-span (math pow (/ stack stacks) stack-power)))]
            (-> (fn [m i]
                  (let [first-index (:first-index m)
                        theta (-> 2 (* (math PI)) (* i) (/ divisions))
                        x (* stack-radius (math cos theta))
                        z (* stack-radius (math sin theta))]
                    (-> m
                        (update :positions (fn [positions]
                                             (-> positions
                                                 (conj! x)
                                                 (conj! 0)
                                                 (conj! z))))
                        (update :normals (fn [normals]
                                           (-> normals
                                               (conj! 0)
                                               (conj! 1)
                                               (conj! 0))))
                        (update :texcoords (fn [texcoords]
                                             (-> texcoords
                                                 (conj! (- 1 (/ i divisions)))
                                                 (conj! (/ stack stacks)))))
                        (update :indices (fn [indices]
                                           (if (and (pos? stack)
                                                    (not= i divisions))
                                             (let [a (+ first-index i 1)
                                                   b (+ first-index i)
                                                   c (-> first-index (+ i) (- points-per-stack))
                                                   d (-> first-index (+ (inc i)) (- points-per-stack))]
                                               (-> indices
                                                   (conj! a)
                                                   (conj! b)
                                                   (conj! c)
                                                   (conj! a)
                                                   (conj! c)
                                                   (conj! d)))
                                             indices))))))
                (reduce m (range (inc divisions)))
                (update :first-index + (inc divisions)))))
        (reduce
          {:first-index 0
           :positions (transient [])
           :normals (transient [])
           :texcoords (transient [])
           :indices (transient [])}
          (range (inc stacks)))
        (dissoc :first-index)
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (update :indices persistent!))))

