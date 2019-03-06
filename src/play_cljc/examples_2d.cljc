(ns play-cljc.examples-2d
  (:require [play-cljc.core :as c]
            [play-cljc.utils :as u]
            [play-cljc.example-utils :as eu]
            [play-cljc.example-data :as data]
            [play-cljc.math :as m]
            #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])
            #?(:clj [dynadoc.example :refer [defexample]]))
  #?(:cljs (:require-macros [dynadoc.example :refer [defexample]])))

;; rand-rects

(defn rand-rects-render [game [entity rects :as state]]
  (eu/resize-example game)
  (c/render-entity game
    {:clear {:color [1 1 1 1] :depth 1}})
  (doseq [{color :color
           [posx posy] :position
           [sx sy] :scale}
          rects]
    (c/render-entity game
      (assoc entity
        :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)}
        :uniforms {'u_color color
                   'u_matrix (->> (m/projection-matrix (eu/get-width game) (eu/get-height game))
                                  (m/multiply-matrices 3 (m/translation-matrix posx posy))
                                  (m/multiply-matrices 3 (m/scaling-matrix sx sy)))})))
  state)

(defn rand-rects-init [game]
  [(c/create-entity game
     {:vertex data/two-d-vertex-shader
      :fragment data/two-d-fragment-shader
      :attributes {'a_position {:data data/rect
                                :type (gl game FLOAT)
                                :size 2}}})
   (for [_ (range 50)]
     {:color [(rand) (rand) (rand) 1]
      :position [(rand-int (eu/get-width game)) (rand-int (eu/get-height game))]
      :scale [(rand-int 300) (rand-int 300)]})])

(defexample play-cljc.examples-2d/rand-rects
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-2d/rand-rects-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-2d/rand-rects-render
      game state)))

;; image

(defn image-render [game [entity {:keys [width height]} :as state]]
  (eu/resize-example game)
  (let [game-width (eu/get-width game)
        game-height (eu/get-height game)
        screen-ratio (/ game-width game-height)
        image-ratio (/ width height)]
    (c/render-entity game
      (assoc entity
        :viewport {:x 0 :y 0 :width game-width :height game-height}
        :uniforms {'u_matrix
                   (->> (m/projection-matrix game-width game-height)
                        (m/multiply-matrices 3 (m/translation-matrix 0 0))
                        (m/multiply-matrices 3 (if (> screen-ratio image-ratio)
                                                 (m/scaling-matrix (* game-height (/ width height)) game-height)
                                                 (m/scaling-matrix game-width (* game-width (/ height width))))))})))
  state)

(defn image-init [game {:keys [data width height] :as image}]
  [(c/create-entity game
     {:vertex data/image-vertex-shader
      :fragment data/image-fragment-shader
      :attributes {'a_position {:data data/rect
                                :type (gl game FLOAT)
                                :size 2}}
      :uniforms {'u_image {:data data
                           :opts {:mip-level 0
                                  :internal-fmt (gl game RGBA)
                                  :width width
                                  :height height
                                  :border 0
                                  :src-fmt (gl game RGBA)
                                  :src-type (gl game UNSIGNED_BYTE)}
                           :params {(gl game TEXTURE_WRAP_S)
                                    (gl game CLAMP_TO_EDGE),
                                    (gl game TEXTURE_WRAP_T)
                                    (gl game CLAMP_TO_EDGE),
                                    (gl game TEXTURE_MIN_FILTER)
                                    (gl game NEAREST),
                                    (gl game TEXTURE_MAG_FILTER)
                                    (gl game NEAREST)}}}
      :clear {:color [1 1 1 1] :depth 1}})
   image])

(defexample play-cljc.examples-2d/image
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)]
    (play-cljc.example-utils/get-image "aintgottaexplainshit.jpg"
      (fn [image]
        (let [state (play-cljc.examples-2d/image-init game image)]
          (play-cljc.example-utils/game-loop
            play-cljc.examples-2d/image-render
            game state))))))

;; translation

(defn translation-render [game [entity *state :as state]]
  (eu/resize-example game)
  (let [{:keys [x y]} @*state]
    (c/render-entity game
      (assoc entity
        :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)}
        :uniforms {'u_matrix (->> (m/projection-matrix (eu/get-width game) (eu/get-height game))
                                  (m/multiply-matrices 3 (m/translation-matrix x y)))})))
  state)

(defn translation-init [game]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type (gl game FLOAT)
                                            :size 2}}
                  :uniforms {'u_color [1 0 0.5 1]}
                  :clear {:color [0 0 0 0] :depth 1}})
        *state (atom {:x 0 :y 0})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.examples-2d/translation
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-2d/translation-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-2d/translation-render
      game state)))

;; rotation

(defn rotation-render [game entity {:keys [tx ty r]}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)}
      :uniforms {'u_matrix (->> (m/projection-matrix (eu/get-width game) (eu/get-height game))
                                (m/multiply-matrices 3 (m/translation-matrix tx ty))
                                (m/multiply-matrices 3 (m/rotation-matrix r))
                                ;; make it rotate around its center
                                (m/multiply-matrices 3 (m/translation-matrix -50 -75)))})))

(defn rotation-init [game]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type (gl game FLOAT)
                                            :size 2}}
                  :uniforms {'u_color [1 0 0.5 1]}
                  :clear {:color [0 0 0 0] :depth 1}})
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse game *state)
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
      :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)}
      :uniforms {'u_matrix (->> (m/projection-matrix (eu/get-width game) (eu/get-height game))
                                (m/multiply-matrices 3 (m/translation-matrix tx ty))
                                (m/multiply-matrices 3 (m/rotation-matrix 0))
                                (m/multiply-matrices 3 (m/scaling-matrix rx ry)))})))

(defn scale-init [game]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type (gl game FLOAT)
                                            :size 2}}
                  :uniforms {'u_color [1 0 0.5 1]}
                  :clear {:color [0 0 0 0] :depth 1}})
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :rx 1 :ry 1})]
    (eu/listen-for-mouse game *state)
    (scale-render game entity @*state)))

(defexample play-cljc.examples-2d/scale
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/scale-init)))

;; rotation-multi

(defn rotation-multi-render [game entity {:keys [tx ty r]}]
  (eu/resize-example game)
  (loop [i 0
         matrix (m/projection-matrix (eu/get-width game) (eu/get-height game))]
    (when (< i 5)
      (let [matrix (->> matrix
                        (m/multiply-matrices 3 (m/translation-matrix tx ty))
                        (m/multiply-matrices 3 (m/rotation-matrix r)))]
        (c/render-entity game
          (assoc entity
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)}
            :uniforms {'u_matrix matrix}))
        (recur (inc i) matrix)))))

(defn rotation-multi-init [game]
  (let [entity (c/create-entity game
                 {:vertex data/two-d-vertex-shader
                  :fragment data/two-d-fragment-shader
                  :attributes {'a_position {:data data/f-2d
                                            :type (gl game FLOAT)
                                            :size 2}}
                  :uniforms {'u_color [1 0 0.5 1]}})
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse game *state)
    (rotation-multi-render game entity @*state)))

(defexample play-cljc.examples-2d/rotation-multi
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-2d/rotation-multi-init)))

