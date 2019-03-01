(ns play-cljc.examples-2d
  (:require [play-cljc.core :as c]
            [play-cljc.utils :as u]
            [play-cljc.client-utils :as cu]
            [goog.events :as events]
            [play-cljc.data :as data])
  (:require-macros [dynadoc.example :refer [defexample]]))

;; rand-rects

(defn rand-rects-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        entity (c/create-entity
                 {:gl gl
                  :vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/rect
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}})]
    (cu/resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (dotimes [_ 50]
      (c/render
        (assoc entity
          :uniforms {'u_color [(rand) (rand) (rand) 1]
                     'u_matrix (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
                                    (u/multiply-matrices 3 (u/translation-matrix (rand-int 300) (rand-int 300)))
                                    (u/multiply-matrices 3 (u/scaling-matrix (rand-int 300) (rand-int 300))))})))))

(defexample play-cljc.examples-2d/rand-rects
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-2d/rand-rects-init)))

;; image

(defn image-init [canvas image]
  (let [gl (.getContext canvas "webgl2")
        entity (c/create-entity
                 {:gl gl
                  :vertex data/image-vertex-shader
                  :fragment data/image-fragment-shader
                  :attributes {'a_position {:data data/rect
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_image {:data image
                                       :opts {:mip-level 0
                                              :internal-fmt gl.RGBA
                                              :src-fmt gl.RGBA
                                              :src-type gl.UNSIGNED_BYTE}
                                       :params {gl.TEXTURE_WRAP_S gl.CLAMP_TO_EDGE
                                                gl.TEXTURE_WRAP_T gl.CLAMP_TO_EDGE
                                                gl.TEXTURE_MIN_FILTER gl.NEAREST
                                                gl.TEXTURE_MAG_FILTER gl.NEAREST}}}})]
    (cu/resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (c/render
      (assoc entity
        :uniforms {'u_matrix
                   (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
                        (u/multiply-matrices 3 (u/translation-matrix 0 0))
                        (u/multiply-matrices 3 (u/scaling-matrix image.width image.height)))}))))

(defn image-load [canvas]
  (let [image (js/Image.)]
    (doto image
      (-> .-src (set! "leaves.jpg"))
      (-> .-onload (set! (fn []
                           (image-init canvas image)))))))

(defexample play-cljc.examples-2d/image
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-2d/image-load)))

;; translation

(defn translation-render [gl canvas entity {:keys [x y]}]
  (cu/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (c/render
      (assoc entity
        :uniforms {'u_matrix (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
                                  (u/multiply-matrices 3 (u/translation-matrix x y)))})))

(defn translation-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        entity (c/create-entity
                 {:gl gl
                  :vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_color [1 0 0.5 1]}})
        *state (atom {:x 0 :y 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds))
              y (- (.-clientY event) (.-top bounds))]
          (translation-render gl canvas entity (swap! *state assoc :x x :y y)))))
    (translation-render gl canvas entity @*state)))

(defexample play-cljc.examples-2d/translation
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-2d/translation-init)))

;; rotation

(defn rotation-render [gl canvas entity {:keys [tx ty r]}]
  (cu/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (c/render
    (assoc entity
      :uniforms {'u_matrix (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
                                (u/multiply-matrices 3 (u/translation-matrix tx ty))
                                (u/multiply-matrices 3 (u/rotation-matrix r))
                                ;; make it rotate around its center
                                (u/multiply-matrices 3 (u/translation-matrix -50 -75)))})))

(defn rotation-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        entity (c/create-entity
                 {:gl gl
                  :vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_color [1 0 0.5 1]}})
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              rx (/ (- (.-clientX event) (.-left bounds) tx)
                    (.-width bounds))
              ry (/ (- (.-clientY event) (.-top bounds) ty)
                    (.-height bounds))]
          (rotation-render gl canvas entity (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-render gl canvas entity @*state)))

(defexample play-cljc.examples-2d/rotation
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-2d/rotation-init)))

;; scale

(defn scale-render [gl canvas entity {:keys [tx ty sx sy]}]
  (cu/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (c/render
    (assoc entity
      :uniforms {'u_matrix (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
                                (u/multiply-matrices 3 (u/translation-matrix tx ty))
                                (u/multiply-matrices 3 (u/rotation-matrix 0))
                                (u/multiply-matrices 3 (u/scaling-matrix sx sy)))})))

(defn scale-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        entity (c/create-entity
                 {:gl gl
                  :vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_color [1 0 0.5 1]}})
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :sx 1 :sy 1})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              sx (/ (- (.-clientX event) (.-left bounds) tx)
                    (.-width bounds))
              sy (/ (- (.-clientY event) (.-top bounds) ty)
                    (.-height bounds))]
          (scale-render gl canvas entity (swap! *state assoc :sx sx :sy sy)))))
    (scale-render gl canvas entity @*state)))

(defexample play-cljc.examples-2d/scale
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-2d/scale-init)))

;; rotation-multi

(defn rotation-multi-render [gl canvas entity {:keys [tx ty r]}]
  (cu/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (loop [i 0
         matrix (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)]
    (when (< i 5)
      (let [matrix (->> matrix
                        (u/multiply-matrices 3 (u/translation-matrix tx ty))
                        (u/multiply-matrices 3 (u/rotation-matrix r)))]
        (c/render (assoc entity :uniforms {'u_matrix matrix}))
        (recur (inc i) matrix)))))

(defn rotation-multi-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        entity (c/create-entity
                 {:gl gl
                  :vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_color [1 0 0.5 1]}})
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              rx (/ (- (.-clientX event) (.-left bounds) tx)
                    (.-width bounds))
              ry (/ (- (.-clientY event) (.-top bounds) ty)
                    (.-height bounds))]
          (rotation-multi-render gl canvas entity (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-multi-render gl canvas entity @*state)))

(defexample play-cljc.examples-2d/rotation-multi
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-2d/rotation-multi-init)))

