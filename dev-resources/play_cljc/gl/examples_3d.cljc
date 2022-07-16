(ns play-cljc.gl.examples-3d
  "3D examples based on content from webgl2fundamentals.org"
  (:require [play-cljc.gl.core :as c]
            [play-cljc.gl.entities-3d :as e]
            [play-cljc.math :as m]
            [play-cljc.transforms :as t]
            [play-cljc.gl.example-utils :as eu]
            [play-cljc.gl.example-data :as data]
            #?(:clj  [play-cljc.macros-java :refer [gl math]]
               :cljs [play-cljc.macros-js :refer-macros [gl math]])
            #?(:clj [dynadoc.example :refer [defexample]]))
  #?(:cljs (:require-macros [dynadoc.example :refer [defexample]])))

(defn- f-entity [game f-data]
  (c/compile game (e/->entity game f-data (mapv #(/ % 255) data/f-3d-colors))))

(defn- transform-f-data [f-data]
  (let [matrix (m/multiply-matrices-3d
                 (m/translation-matrix-3d -50 -75 -15)
                 (m/x-rotation-matrix-3d (math PI)))]
    (reduce
      (fn [positions i]
        (let [v (m/transform-vector matrix
                  [(nth f-data (+ i 0))
                   (nth f-data (+ i 1))
                   (nth f-data (+ i 2))
                   1])]
          (-> positions
              (assoc (+ i 0) (nth v 0))
              (assoc (+ i 1) (nth v 1))
              (assoc (+ i 2) (nth v 2)))))
      f-data
      (range 0 (count f-data) 3))))

;; translation-3d

(defn translation-3d-example [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        *state (atom {:x 0 :y 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample translation-3d-example
  {:with-card card
   :with-focus [focus (play-cljc.gl.core/render game
                        (-> entity
                            (assoc
                              :clear {:color [1 1 1 1] :depth 1}
                              :viewport {:x 0 :y 0 :width game-width :height game-height})
                            (play-cljc.transforms/project 0 game-width game-height 0 400 -400)
                            (play-cljc.transforms/translate x y 0)
                            (play-cljc.transforms/rotate (play-cljc.math/deg->rad 40) :x)
                            (play-cljc.transforms/rotate (play-cljc.math/deg->rad 25) :y)
                            (play-cljc.transforms/rotate (play-cljc.math/deg->rad 325) :z)))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/translation-3d-example)
       (play-cljc.gl.example-utils/game-loop
         (fn translation-3d-render [{:keys [entity *state] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (let [{:keys [x y]} @*state
                 game-width (play-cljc.gl.example-utils/get-width game)
                 game-height (play-cljc.gl.example-utils/get-height game)]
             focus)
           game))))

;; rotation-3d

(defn rotation-3d-example [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample rotation-3d-example
  {:with-card card
   :with-focus [focus (play-cljc.gl.core/render game
                        (-> entity
                            (assoc
                              :clear {:color [1 1 1 1] :depth 1}
                              :viewport {:x 0 :y 0 :width game-width :height game-height})
                            (play-cljc.transforms/project 0 game-width game-height 0 400 -400)
                            (play-cljc.transforms/translate tx ty 0)
                            (play-cljc.transforms/rotate r :x)
                            (play-cljc.transforms/rotate r :y)
                            (play-cljc.transforms/rotate r :z)
                            (play-cljc.transforms/translate -50 -75 0)))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/rotation-3d-example)
       (play-cljc.gl.example-utils/game-loop
         (fn rotation-3d-render [{:keys [entity *state] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (let [{:keys [tx ty r]} @*state
                 game-width (play-cljc.gl.example-utils/get-width game)
                 game-height (play-cljc.gl.example-utils/get-height game)]
             focus)
           game))))

;; scale-3d

(defn scale-3d-example [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :rx 1 :ry 1})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample scale-3d-example
  {:with-card card
   :with-focus [focus (play-cljc.gl.core/render game
                        (-> entity
                            (assoc
                              :clear {:color [1 1 1 1] :depth 1}
                              :viewport {:x 0 :y 0 :width game-width :height game-height})
                            (play-cljc.transforms/project 0 game-width game-height 0 400 -400)
                            (play-cljc.transforms/translate tx ty 0)
                            (play-cljc.transforms/rotate (play-cljc.math/deg->rad 40) :x)
                            (play-cljc.transforms/rotate (play-cljc.math/deg->rad 25) :y)
                            (play-cljc.transforms/rotate (play-cljc.math/deg->rad 325) :z)
                            (play-cljc.transforms/scale rx ry 1)))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/scale-3d-example)
       (play-cljc.gl.example-utils/game-loop
         (fn scale-3d-render [{:keys [entity *state] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (let [{:keys [tx ty rx ry]} @*state
                 game-width (play-cljc.gl.example-utils/get-width game)
                 game-height (play-cljc.gl.example-utils/get-height game)]
             focus)
           game))))

;; perspective-3d

(defn perspective-3d-example [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        *state (atom {:cx 0 :cy 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample perspective-3d-example
  {:with-card card
   :with-focus [focus (play-cljc.gl.core/render game
                        (-> entity
                            (assoc
                              :clear {:color [1 1 1 1] :depth 1}
                              :viewport {:x 0 :y 0 :width game-width :height game-height})
                            (play-cljc.transforms/project
                              (play-cljc.math/deg->rad 60)
                              (/ game-width game-height) 1 2000)
                            (play-cljc.transforms/translate cx cy -150)
                            (play-cljc.transforms/rotate (play-cljc.math/deg->rad 180) :x)
                            (play-cljc.transforms/rotate 0 :y)
                            (play-cljc.transforms/rotate 0 :z)))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/perspective-3d-example)
       (play-cljc.gl.example-utils/game-loop
         (fn perspective-3d-render [{:keys [entity *state] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (let [{:keys [cx cy]} @*state
                 game-width (play-cljc.gl.example-utils/get-width game)
                 game-height (play-cljc.gl.example-utils/get-height game)]
             focus)
           game))))

;; perspective-camera-3d

(defn perspective-camera-3d-example [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game (transform-f-data data/f-3d))
        *state (atom {:cr 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample perspective-camera-3d-example
  {:with-card card
   :with-focus [focus (dotimes [i num-fs]
                        (let [angle (/ (* i PI 2) num-fs)
                              x (* (cos angle) radius)
                              z (* (sin angle) radius)]
                          (play-cljc.gl.core/render game
                            (play-cljc.transforms/translate entity x 0 z))))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/perspective-camera-3d-example)
       (play-cljc.gl.example-utils/game-loop
         (fn perspective-camera-3d-render [{:keys [entity *state] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (play-cljc.gl.core/render game
             {:clear {:color [1 1 1 1] :depth 1}})
           (let [{:keys [cr]} @*state
                 game-width (play-cljc.gl.example-utils/get-width game)
                 game-height (play-cljc.gl.example-utils/get-height game)
                 radius 200
                 num-fs 5
                 camera (-> (play-cljc.gl.entities-3d/->camera)
                            (play-cljc.transforms/rotate cr :y)
                            (play-cljc.transforms/translate 0 0 (* radius 1.5)))
                 entity (-> entity
                            (assoc :viewport {:x 0 :y 0 :width game-width :height game-height})
                            (play-cljc.transforms/project (play-cljc.math/deg->rad 60)
                              (/ game-width game-height) 1 2000)
                            (play-cljc.transforms/invert camera))
                 PI play-cljc.gl.example-utils/PI
                 sin play-cljc.gl.example-utils/sin
                 cos play-cljc.gl.example-utils/cos]
             focus)
           game))))

;; perspective-camera-target-3d

(defn perspective-camera-target-3d-example [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game (transform-f-data data/f-3d))
        *state (atom {:cr 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample perspective-camera-target-3d-example
  {:with-card card
   :with-focus [focus (dotimes [i num-fs]
                        (let [angle (/ (* i PI 2) num-fs)
                              x (* (cos angle) radius)
                              z (* (sin angle) radius)]
                          (play-cljc.gl.core/render game
                            (play-cljc.transforms/translate entity x 0 z))))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/perspective-camera-target-3d-example)
       (play-cljc.gl.example-utils/game-loop
         (fn perspective-camera-target-3d-render [{:keys [entity *state] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (play-cljc.gl.core/render game
             {:clear {:color [1 1 1 1] :depth 1}})
           (let [{:keys [cr]} @*state
                 game-width (play-cljc.gl.example-utils/get-width game)
                 game-height (play-cljc.gl.example-utils/get-height game)
                 radius 200
                 num-fs 5
                 camera (-> (play-cljc.gl.entities-3d/->camera)
                            (play-cljc.transforms/rotate cr :y)
                            (play-cljc.transforms/translate 0 0 (* radius 1.5))
                            (play-cljc.transforms/look-at [radius 0 0] [0 1 0]))
                 entity (-> entity
                            (assoc :viewport {:x 0 :y 0 :width game-width :height game-height})
                            (play-cljc.transforms/project (play-cljc.math/deg->rad 60)
                              (/ game-width game-height) 1 2000)
                            (play-cljc.transforms/invert camera))
                 PI play-cljc.gl.example-utils/PI
                 sin play-cljc.gl.example-utils/sin
                 cos play-cljc.gl.example-utils/cos]
             focus)
           game))))

;; perspective-animation-3d

(defn perspective-animation-3d-example [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)
               :rz (m/deg->rad 320)}]
    (assoc game :entity entity :state state)))

(defexample perspective-animation-3d-example
  {:with-card card
   :with-focus [focus (play-cljc.gl.core/render game
                        (-> entity
                            (assoc
                              :clear {:color [1 1 1 1] :depth 1}
                              :viewport {:x 0 :y 0 :width game-width :height game-height})
                            (play-cljc.transforms/project (play-cljc.math/deg->rad 60)
                              (/ game-width game-height) 1 2000)
                            (play-cljc.transforms/translate 0 0 -360)
                            (play-cljc.transforms/rotate rx :x)
                            (play-cljc.transforms/rotate ry :y)
                            (play-cljc.transforms/rotate rz :z)))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/perspective-animation-3d-example)
       (play-cljc.gl.example-utils/game-loop
         (fn perspective-animation-3d-render [{:keys [entity state delta-time] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (let [game-width (play-cljc.gl.example-utils/get-width game)
                 game-height (play-cljc.gl.example-utils/get-height game)
                 {:keys [rx ry rz]} state]
             focus)
           (update-in game [:state :ry] + (* 1.2 delta-time))))))

;; perspective-texture-3d

(defn perspective-texture-3d-example [game {:keys [data width height]}]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (->> {:vertex data/texture-vertex-shader
                     :fragment data/texture-fragment-shader
                     :attributes {'a_position {:data (transform-f-data data/f-3d)
                                               :type (gl game FLOAT)
                                               :size 3}
                                  'a_texcoord {:data data/f-texcoords
                                               :type (gl game FLOAT)
                                               :size 2
                                               :normalize true}}
                     :uniforms {'u_texture {:data data
                                            :opts {:mip-level 0
                                                   :internal-fmt (gl game RGBA)
                                                   :width width
                                                   :height height
                                                   :border 0
                                                   :src-fmt (gl game RGBA)
                                                   :src-type (gl game UNSIGNED_BYTE)}
                                            :mipmap true}}
                     :clear {:color [0 0 0 0] :depth 1}}
                    e/map->ThreeDEntity
                    (c/compile game))
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)}]
    (assoc game :entity entity :state state)))

(defexample perspective-texture-3d-example
  {:with-card card
   :with-focus [focus (play-cljc.gl.core/render game
                        (-> entity
                            (assoc :viewport {:x 0 :y 0 :width game-width :height game-height})
                            (play-cljc.transforms/project (play-cljc.math/deg->rad 60)
                              (/ game-width game-height) 1 2000)
                            (play-cljc.transforms/invert camera)
                            (play-cljc.transforms/rotate rx :x)
                            (play-cljc.transforms/rotate ry :y)))]}
  (let [game (play-cljc.gl.example-utils/init-example card)]
    (play-cljc.gl.example-utils/get-image "f-texture.png"
      (fn [image]
        (->> (play-cljc.gl.examples-3d/perspective-texture-3d-example game image)
             (play-cljc.gl.example-utils/game-loop
               (fn perspective-texture-3d-render [{:keys [entity state delta-time] :as game}]
                 (play-cljc.gl.example-utils/resize-example game)
                 (let [camera (-> (play-cljc.gl.entities-3d/->camera)
                                  (play-cljc.transforms/translate 0 0 200)
                                  (play-cljc.transforms/look-at [0 0 0] [0 1 0]))
                       game-width (play-cljc.gl.example-utils/get-width game)
                       game-height (play-cljc.gl.example-utils/get-height game)
                       {:keys [rx ry]} state]
                   focus)
                 (-> game
                     (update-in [:state :rx] + (* 1.2 delta-time))
                     (update-in [:state :ry] + (* 0.7 delta-time))))))))))

;; perspective-texture-data-3d

(defn perspective-texture-data-3d-example [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (->> {:vertex data/texture-vertex-shader
                     :fragment data/texture-fragment-shader
                     :attributes {'a_position {:data data/cube
                                               :type (gl game FLOAT)
                                               :size 3}
                                  'a_texcoord {:data data/cube-texcoords
                                               :type (gl game FLOAT)
                                               :size 2
                                               :normalize true}}
                     :uniforms {'u_texture {:data [128 64 128 0 192 0]
                                            :opts {:mip-level 0
                                                   :internal-fmt (gl game R8)
                                                   :width 3
                                                   :height 2
                                                   :border 0
                                                   :src-fmt (gl game RED)
                                                   :src-type (gl game UNSIGNED_BYTE)}
                                            :alignment 1
                                            :params {(gl game TEXTURE_WRAP_S)
                                                     (gl game CLAMP_TO_EDGE),
                                                     (gl game TEXTURE_WRAP_T)
                                                     (gl game CLAMP_TO_EDGE),
                                                     (gl game TEXTURE_MIN_FILTER)
                                                     (gl game NEAREST),
                                                     (gl game TEXTURE_MAG_FILTER)
                                                     (gl game NEAREST)}}}
                     :clear {:color [1 1 1 1] :depth 1}}
                    e/map->ThreeDEntity
                    (c/compile game))
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)}]
    (assoc game :entity entity :state state)))

(defexample perspective-texture-data-3d-example
  {:with-card card
   :with-focus [focus (play-cljc.gl.core/render game
                        (-> entity
                            (assoc :viewport {:x 0 :y 0 :width game-width :height game-height})
                            (play-cljc.transforms/project (play-cljc.math/deg->rad 60)
                              (/ game-width game-height) 1 2000)
                            (play-cljc.transforms/invert camera)
                            (play-cljc.transforms/rotate rx :x)
                            (play-cljc.transforms/rotate ry :y)))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/perspective-texture-data-3d-example)
       (play-cljc.gl.example-utils/game-loop
         (fn perspective-texture-data-3d-render [{:keys [entity state delta-time] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (let [camera (-> (play-cljc.gl.entities-3d/->camera)
                            (play-cljc.transforms/translate 0 0 2)
                            (play-cljc.transforms/look-at [0 0 0] [0 1 0]))
                 game-width (play-cljc.gl.example-utils/get-width game)
                 game-height (play-cljc.gl.example-utils/get-height game)
                 {:keys [rx ry]} state]
             focus)
           (-> game
               (update-in [:state :rx] + (* 1.2 delta-time))
               (update-in [:state :ry] + (* 0.7 delta-time)))))))

;; perspective-texture-meta-3d

(defn perspective-texture-meta-3d-example [target-width target-height game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (->> {:vertex data/texture-vertex-shader
                     :fragment data/texture-fragment-shader
                     :attributes {'a_position {:data data/cube
                                               :type (gl game FLOAT)
                                               :size 3}
                                  'a_texcoord {:data data/cube-texcoords
                                               :type (gl game FLOAT)
                                               :size 2
                                               :normalize true}}
                     :uniforms {'u_texture {:data nil
                                            :opts {:mip-level 0
                                                   :internal-fmt (gl game RGBA)
                                                   :width target-width
                                                   :height target-height
                                                   :border 0
                                                   :src-fmt (gl game RGBA)
                                                   :src-type (gl game UNSIGNED_BYTE)}
                                            :params {(gl game TEXTURE_WRAP_S)
                                                     (gl game CLAMP_TO_EDGE),
                                                     (gl game TEXTURE_WRAP_T)
                                                     (gl game CLAMP_TO_EDGE),
                                                     (gl game TEXTURE_MIN_FILTER)
                                                     (gl game LINEAR)}}}
                     :clear {:color [1 1 1 1] :depth 1}}
                    e/map->ThreeDEntity
                    (c/compile game))
        inner-entity (->> {:vertex data/texture-vertex-shader
                           :fragment data/texture-fragment-shader
                           :attributes {'a_position {:data data/cube
                                                     :type (gl game FLOAT)
                                                     :size 3}
                                        'a_texcoord {:data data/cube-texcoords
                                                     :type (gl game FLOAT)
                                                     :size 2
                                                     :normalize true}}
                           :uniforms {'u_texture {:data [128 64 128 0 192 0]
                                                  :opts {:mip-level 0
                                                         :internal-fmt (gl game R8)
                                                         :width 3
                                                         :height 2
                                                         :border 0
                                                         :src-fmt (gl game RED)
                                                         :src-type (gl game UNSIGNED_BYTE)}
                                                  :alignment 1
                                                  :params {(gl game TEXTURE_WRAP_S)
                                                           (gl game CLAMP_TO_EDGE),
                                                           (gl game TEXTURE_WRAP_T)
                                                           (gl game CLAMP_TO_EDGE),
                                                           (gl game TEXTURE_MIN_FILTER)
                                                           (gl game NEAREST),
                                                           (gl game TEXTURE_MAG_FILTER)
                                                           (gl game NEAREST)}}}
                           :clear {:color [0 0 1 1] :depth 1}}
                          e/map->ThreeDEntity
                          (c/compile game))
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)}]
    (assoc game :entity entity :inner-entity inner-entity :state state)))

(defexample perspective-texture-meta-3d-example
  {:with-card card
   :with-focus [focus (let [camera (-> (play-cljc.gl.entities-3d/->camera)
                                       (play-cljc.transforms/translate 0 0 2)
                                       (play-cljc.transforms/look-at [0 0 0] [0 1 0]))]
                        (-> entity
                            (play-cljc.transforms/project (play-cljc.math/deg->rad 60) aspect 1 2000)
                            (play-cljc.transforms/invert camera)
                            (play-cljc.transforms/rotate rx :x)
                            (play-cljc.transforms/rotate ry :y)))]}
  (let [target-width 256
        target-height 256
        render-cube (fn [entity {:keys [rx ry]} aspect]
                      focus)]
    (->> (play-cljc.gl.example-utils/init-example card)
         (play-cljc.gl.examples-3d/perspective-texture-meta-3d-example target-width target-height)
         (play-cljc.gl.example-utils/game-loop
           (fn perspective-texture-meta-3d-render [{:keys [entity inner-entity state delta-time] :as game}]
             (play-cljc.gl.example-utils/resize-example game)
             (let [game-width (play-cljc.gl.example-utils/get-width game)
                   game-height (play-cljc.gl.example-utils/get-height game)]
               (play-cljc.gl.core/render game
                 (-> entity
                     (assoc :viewport {:x 0 :y 0 :width game-width :height game-height})
                     (render-cube state (/ game-width game-height))
                     (assoc :render-to-texture {'u_texture
                                                [(-> inner-entity
                                                     (assoc :viewport {:x 0 :y 0
                                                                       :width target-width
                                                                       :height target-height})
                                                     (render-cube state (/ target-width target-height)))]}))))
             (-> game
                 (update-in [:state :rx] + (* 1.2 delta-time))
                 (update-in [:state :ry] + (* 0.7 delta-time))))))))

