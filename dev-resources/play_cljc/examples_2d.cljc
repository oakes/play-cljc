(ns play-cljc.examples-2d
  (:require [play-cljc.core :as c]
            [play-cljc.utils :as u]
            [play-cljc.example-utils :as eu]
            [play-cljc.example-data :as data]
            [play-cljc.math :as m]
            [play-cljc.transforms :as t]
            #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])
            #?(:clj [dynadoc.example :refer [defexample]]))
  #?(:cljs (:require-macros [dynadoc.example :refer [defexample]])))

(defrecord TwoDEntity [])

(extend-type TwoDEntity
  t/IProject
  (project [entity {:keys [width height]}]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 3 (m/projection-matrix width height) %)))
  t/ITranslate
  (translate [entity {:keys [x y]}]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 3 (m/translation-matrix x y) %)))
  t/IScale
  (scale [entity {:keys [width height]}]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 3 (m/scaling-matrix width height) %)))
  t/IRotate
  (rotate [entity {:keys [angle]}]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 3 (m/rotation-matrix angle) %))))

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
      (-> entity
          (assoc
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)}
            :uniforms {'u_color color
                       'u_matrix (m/identity-matrix)})
          (t/project {:width (eu/get-width game) :height (eu/get-height game)})
          (t/translate {:x posx :y posy})
          (t/scale {:width sx :height sy}))))
  state)

(defn rand-rects-init [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  [(->> {:vertex data/two-d-vertex-shader
         :fragment data/two-d-fragment-shader
         :attributes {'a_position {:data data/rect
                                   :type (gl game FLOAT)
                                   :size 2}}}
        (c/create-entity game)
        map->TwoDEntity)
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
        image-ratio (/ width height)
        img-scale (if (> screen-ratio image-ratio)
                    {:width (* game-height (/ width height)) :height game-height}
                    {:width game-width :height (* game-width (/ height width))})]
    (c/render-entity game
      (-> entity
          (assoc
            :viewport {:x 0 :y 0 :width game-width :height game-height}
            :uniforms {'u_matrix (m/identity-matrix)})
          (t/project {:width game-width :height game-height})
          (t/translate {:x 0 :y 0})
          (t/scale img-scale))))
  state)

(defn image-init [game {:keys [data width height] :as image}]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  [(->> {:vertex data/image-vertex-shader
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
         :clear {:color [1 1 1 1] :depth 1}}
        (c/create-entity game)
        map->TwoDEntity)
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
      (-> entity
          (assoc
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)}
            :uniforms {'u_matrix (m/identity-matrix)})
          (t/project {:width (eu/get-width game) :height (eu/get-height game)})
          (t/translate {:x x :y y}))))
  state)

(defn translation-init [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (->> {:vertex data/two-d-vertex-shader
                     :fragment data/two-d-fragment-shader
                     :attributes {'a_position {:data data/f-2d
                                               :type (gl game FLOAT)
                                               :size 2}}
                     :uniforms {'u_color [1 0 0.5 1]}
                     :clear {:color [0 0 0 0] :depth 1}}
                    (c/create-entity game)
                    map->TwoDEntity)
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

(defn rotation-render [game [entity *state :as state]]
  (eu/resize-example game)
  (let [{:keys [tx ty r]} @*state]
    (c/render-entity game
      (-> entity
          (assoc
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)}
            :uniforms {'u_matrix (m/identity-matrix)})
          (t/project {:width (eu/get-width game) :height (eu/get-height game)})
          (t/translate {:x tx :y ty})
          (t/rotate {:angle r})
          ;; make it rotate around its center
          (t/translate {:x -50 :y -75}))))
  state)

(defn rotation-init [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (->> {:vertex data/two-d-vertex-shader
                     :fragment data/two-d-fragment-shader
                     :attributes {'a_position {:data data/f-2d
                                               :type (gl game FLOAT)
                                               :size 2}}
                     :uniforms {'u_color [1 0 0.5 1]}
                     :clear {:color [0 0 0 0] :depth 1}}
                    (c/create-entity game)
                    map->TwoDEntity)
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.examples-2d/rotation
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-2d/rotation-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-2d/rotation-render
      game state)))

;; scale

(defn scale-render [game [entity *state :as state]]
  (eu/resize-example game)
  (let [{:keys [tx ty rx ry]} @*state]
    (c/render-entity game
      (-> entity
          (assoc
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)}
            :uniforms {'u_matrix (m/identity-matrix)})
          (t/project {:width (eu/get-width game) :height (eu/get-height game)})
          (t/translate {:x tx :y ty})
          (t/rotate {:angle 0})
          (t/scale {:width rx :height ry}))))
  state)

(defn scale-init [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (->> {:vertex data/two-d-vertex-shader
                     :fragment data/two-d-fragment-shader
                     :attributes {'a_position {:data data/f-2d
                                               :type (gl game FLOAT)
                                               :size 2}}
                     :uniforms {'u_color [1 0 0.5 1]}
                     :clear {:color [0 0 0 0] :depth 1}}
                    (c/create-entity game)
                    map->TwoDEntity)
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :rx 1 :ry 1})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.examples-2d/scale
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-2d/scale-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-2d/scale-render
      game state)))

;; rotation-multi

(defn rotation-multi-render [game [entity *state :as state]]
  (eu/resize-example game)
  (c/render-entity game
    {:clear {:color [1 1 1 1] :depth 1}})
  (let [{:keys [tx ty r]} @*state]
    (loop [i 0
           entity (-> entity
                      (assoc
                        :uniforms {'u_matrix (m/identity-matrix)}
                        :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
                      (t/project {:width (eu/get-width game) :height (eu/get-height game)}))]
      (when (< i 5)
        (let [entity (-> entity
                         (t/translate {:x tx :y ty})
                         (t/rotate {:angle r}))]
          (c/render-entity game entity)
          (recur (inc i) entity)))))
  state)

(defn rotation-multi-init [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (->> {:vertex data/two-d-vertex-shader
                     :fragment data/two-d-fragment-shader
                     :attributes {'a_position {:data data/f-2d
                                               :type (gl game FLOAT)
                                               :size 2}}
                     :uniforms {'u_color [1 0 0.5 1]}}
                    (c/create-entity game)
                    map->TwoDEntity)
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.examples-2d/rotation-multi
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-2d/rotation-multi-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-2d/rotation-multi-render
      game state)))

