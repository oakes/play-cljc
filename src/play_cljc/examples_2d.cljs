(ns play-cljc.examples-2d
  (:require [play-cljc.core :as c]
            [play-cljc.utils :as u]
            [play-cljc.example-utils :as eu]
            [play-cljc.example-data :as data])
  (:require-macros [dynadoc.example :refer [defexample]]))

;; rand-rects

(defn rand-rects-init [game]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/rect
                                            :type (u/get-enum game :float)
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}})]
    (eu/resize-example game)
    (dotimes [_ 50]
      (c/render-entity game
        (assoc entity
          :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
          :uniforms {'u_color [(rand) (rand) (rand) 1]
                     'u_matrix (->> (u/projection-matrix (u/get-width game) (u/get-height game))
                                    (u/multiply-matrices 3 (u/translation-matrix (rand-int 300) (rand-int 300)))
                                    (u/multiply-matrices 3 (u/scaling-matrix (rand-int 300) (rand-int 300))))})))))

(defexample play-cljc.examples-2d/rand-rects
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/rand-rects-init)))

;; image

(defn image-init [game image]
  (let [entity (c/create-entity game
                 {:vertex data/image-vertex-shader
                  :fragment data/image-fragment-shader
                  :attributes {'a_position {:data data/rect
                                            :type (u/get-enum game :float)
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_image {:data image
                                       :opts {:mip-level 0
                                              :internal-fmt (u/get-enum game :rgba)
                                              :src-fmt (u/get-enum game :rgba)
                                              :src-type (u/get-enum game :unsigned-byte)}
                                       :params {(u/get-enum game :texture-wrap-s)
                                                (u/get-enum game :clamp-to-edge),
                                                (u/get-enum game :texture-wrap-t)
                                                (u/get-enum game :clamp-to-edge),
                                                (u/get-enum game :texture-min-filter)
                                                (u/get-enum game :nearest),
                                                (u/get-enum game :texture-mag-filter)
                                                (u/get-enum game :nearest)}}}
                  :clear {:color [0 0 0 0] :depth 1}})]
    (eu/resize-example game)
    (c/render-entity game
      (assoc entity
        :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
        :uniforms {'u_matrix
                   (->> (u/projection-matrix (u/get-width game) (u/get-height game))
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

(defn translation-render [game entity {:keys [x y]}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
      :uniforms {'u_matrix (->> (u/projection-matrix (u/get-width game) (u/get-height game))
                                (u/multiply-matrices 3 (u/translation-matrix x y)))})))

(defn translation-init [game]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type (u/get-enum game :float)
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_color [1 0 0.5 1]}
                  :clear {:color [0 0 0 0] :depth 1}})
        *state (atom {:x 0 :y 0})]
    (eu/listen-for-mouse game #(translation-render game entity (swap! *state merge %)))
    (translation-render game entity @*state)))

(defexample play-cljc.examples-2d/translation
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/translation-init)))

;; rotation

(defn rotation-render [game entity {:keys [tx ty r]}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
      :uniforms {'u_matrix (->> (u/projection-matrix (u/get-width game) (u/get-height game))
                                (u/multiply-matrices 3 (u/translation-matrix tx ty))
                                (u/multiply-matrices 3 (u/rotation-matrix r))
                                ;; make it rotate around its center
                                (u/multiply-matrices 3 (u/translation-matrix -50 -75)))})))

(defn rotation-init [game]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type (u/get-enum game :float)
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_color [1 0 0.5 1]}
                  :clear {:color [0 0 0 0] :depth 1}})
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse (merge game @*state)
      #(rotation-render game entity (swap! *state merge %)))
    (rotation-render game entity @*state)))

(defexample play-cljc.examples-2d/rotation
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/rotation-init)))

;; scale

(defn scale-render [game entity {:keys [tx ty rx ry]}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
      :uniforms {'u_matrix (->> (u/projection-matrix (u/get-width game) (u/get-height game))
                                (u/multiply-matrices 3 (u/translation-matrix tx ty))
                                (u/multiply-matrices 3 (u/rotation-matrix 0))
                                (u/multiply-matrices 3 (u/scaling-matrix rx ry)))})))

(defn scale-init [game]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type (u/get-enum game :float)
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_color [1 0 0.5 1]}
                  :clear {:color [0 0 0 0] :depth 1}})
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :rx 1 :ry 1})]
    (eu/listen-for-mouse (merge game @*state)
      #(scale-render game entity (swap! *state merge %)))
    (scale-render game entity @*state)))

(defexample play-cljc.examples-2d/scale
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/scale-init)))

;; rotation-multi

(defn rotation-multi-render [game entity {:keys [tx ty r]}]
  (eu/resize-example game)
  (loop [i 0
         matrix (u/projection-matrix (u/get-width game) (u/get-height game))]
    (when (< i 5)
      (let [matrix (->> matrix
                        (u/multiply-matrices 3 (u/translation-matrix tx ty))
                        (u/multiply-matrices 3 (u/rotation-matrix r)))]
        (c/render-entity game
          (assoc entity
            :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
            :uniforms {'u_matrix matrix}))
        (recur (inc i) matrix)))))

(defn rotation-multi-init [game]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type (u/get-enum game :float)
                                            :size 2
                                            :normalize false
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_color [1 0 0.5 1]}})
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse (merge game @*state)
      #(rotation-multi-render game entity (swap! *state merge %)))
    (rotation-multi-render game entity @*state)))

(defexample play-cljc.examples-2d/rotation-multi
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/rotation-multi-init)))

