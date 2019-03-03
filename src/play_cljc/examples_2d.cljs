(ns play-cljc.examples-2d
  (:require [play-cljc.core :as c]
            [play-cljc.utils :as u]
            [play-cljc.example-utils :as eu]
            [goog.events :as events]
            [play-cljc.data :as data])
  (:require-macros [dynadoc.example :refer [defexample]]))

;; rand-rects

(defn rand-rects-init [{:keys [gl] :as game}]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/rect
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}})]
    (eu/resize-example game)
    (dotimes [_ 50]
      (c/render-entity game
        (assoc entity
          :viewport {:x 0 :y 0 :width gl.canvas.clientWidth :height gl.canvas.clientHeight}
          :uniforms {'u_color [(rand) (rand) (rand) 1]
                     'u_matrix (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
                                    (u/multiply-matrices 3 (u/translation-matrix (rand-int 300) (rand-int 300)))
                                    (u/multiply-matrices 3 (u/scaling-matrix (rand-int 300) (rand-int 300))))})))))

(defexample play-cljc.examples-2d/rand-rects
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/rand-rects-init)))

;; image

(defn image-init [{:keys [gl] :as game} image]
  (let [entity (c/create-entity game
                 {:vertex data/image-vertex-shader
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
                                                gl.TEXTURE_MAG_FILTER gl.NEAREST}}}
                  :clear {:color [0 0 0 0] :depth 1}})]
    (eu/resize-example game)
    (c/render-entity game
      (assoc entity
        :viewport {:x 0 :y 0 :width gl.canvas.clientWidth :height gl.canvas.clientHeight}
        :uniforms {'u_matrix
                   (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
                        (u/multiply-matrices 3 (u/translation-matrix 0 0))
                        (u/multiply-matrices 3 (u/scaling-matrix image.width image.height)))}))))

(defn image-load [game]
  (let [image (js/Image.)]
    (doto image
      (-> .-src (set! "leaves.jpg"))
      (-> .-onload (set! (fn []
                           (image-init game image)))))))

(defexample play-cljc.examples-2d/image
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/image-load)))

;; translation

(defn translation-render [{:keys [gl] :as game} entity {:keys [x y]}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width gl.canvas.clientWidth :height gl.canvas.clientHeight}
      :uniforms {'u_matrix (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
                                (u/multiply-matrices 3 (u/translation-matrix x y)))})))

(defn translation-init [{:keys [gl canvas] :as game}]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_color [1 0 0.5 1]}
                  :clear {:color [0 0 0 0] :depth 1}})
        *state (atom {:x 0 :y 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds))
              y (- (.-clientY event) (.-top bounds))]
          (translation-render game entity (swap! *state assoc :x x :y y)))))
    (translation-render game entity @*state)))

(defexample play-cljc.examples-2d/translation
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/translation-init)))

;; rotation

(defn rotation-render [{:keys [gl] :as game} entity {:keys [tx ty r]}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width gl.canvas.clientWidth :height gl.canvas.clientHeight}
      :uniforms {'u_matrix (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
                                (u/multiply-matrices 3 (u/translation-matrix tx ty))
                                (u/multiply-matrices 3 (u/rotation-matrix r))
                                ;; make it rotate around its center
                                (u/multiply-matrices 3 (u/translation-matrix -50 -75)))})))

(defn rotation-init [{:keys [gl canvas] :as game}]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_color [1 0 0.5 1]}
                  :clear {:color [0 0 0 0] :depth 1}})
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
          (rotation-render game entity (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-render game entity @*state)))

(defexample play-cljc.examples-2d/rotation
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/rotation-init)))

;; scale

(defn scale-render [{:keys [gl] :as game} entity {:keys [tx ty sx sy]}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width gl.canvas.clientWidth :height gl.canvas.clientHeight}
      :uniforms {'u_matrix (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
                                (u/multiply-matrices 3 (u/translation-matrix tx ty))
                                (u/multiply-matrices 3 (u/rotation-matrix 0))
                                (u/multiply-matrices 3 (u/scaling-matrix sx sy)))})))

(defn scale-init [{:keys [gl canvas] :as game}]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_color [1 0 0.5 1]}
                  :clear {:color [0 0 0 0] :depth 1}})
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
          (scale-render game entity (swap! *state assoc :sx sx :sy sy)))))
    (scale-render game entity @*state)))

(defexample play-cljc.examples-2d/scale
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/scale-init)))

;; rotation-multi

(defn rotation-multi-render [{:keys [gl] :as game} entity {:keys [tx ty r]}]
  (eu/resize-example game)
  (loop [i 0
         matrix (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)]
    (when (< i 5)
      (let [matrix (->> matrix
                        (u/multiply-matrices 3 (u/translation-matrix tx ty))
                        (u/multiply-matrices 3 (u/rotation-matrix r)))]
        (c/render-entity game
          (assoc entity
            :viewport {:x 0 :y 0 :width gl.canvas.clientWidth :height gl.canvas.clientHeight}
            :uniforms {'u_matrix matrix}))
        (recur (inc i) matrix)))))

(defn rotation-multi-init [{:keys [gl canvas] :as game}]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
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
          (rotation-multi-render game entity (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-multi-render game entity @*state)))

(defexample play-cljc.examples-2d/rotation-multi
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/rotation-multi-init)))

