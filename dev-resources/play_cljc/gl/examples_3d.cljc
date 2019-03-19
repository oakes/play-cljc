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

(defn f-entity [game f-data]
  (c/compile game (e/->entity game f-data (mapv #(/ % 255) data/f-3d-colors))))

(defn transform-f-data [f-data]
  (let [matrix (m/multiply-matrices 4
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

(defn translation-3d-render [{:keys [entity *state] :as game}]
  (eu/resize-example game)
  (let [{:keys [x y]} @*state]
    (c/render game
      (-> entity
          (assoc
            :clear {:color [1 1 1 1] :depth 1}
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project 0 (eu/get-width game) (eu/get-height game) 0 400 -400)
          (t/translate x y 0)
          (t/rotate (m/deg->rad 40) :x)
          (t/rotate (m/deg->rad 25) :y)
          (t/rotate (m/deg->rad 325) :z))))
  game)

(defn translation-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        *state (atom {:x 0 :y 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample play-cljc.gl.examples-3d/translation-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/translation-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-3d/translation-3d-render)))

;; rotation-3d

(defn rotation-3d-render [{:keys [entity *state] :as game}]
  (eu/resize-example game)
  (let [{:keys [tx ty r]} @*state]
    (c/render game
      (-> entity
          (assoc
            :clear {:color [1 1 1 1] :depth 1}
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project 0 (eu/get-width game) (eu/get-height game) 0 400 -400)
          (t/translate tx ty 0)
          (t/rotate r :x)
          (t/rotate r :y)
          (t/rotate r :z)
          ;; make it rotate around its center
          (t/translate -50 -75 0))))
  game)

(defn rotation-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample play-cljc.gl.examples-3d/rotation-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/rotation-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-3d/rotation-3d-render)))

;; scale-3d

(defn scale-3d-render [{:keys [entity *state] :as game}]
  (eu/resize-example game)
  (let [{:keys [tx ty rx ry]} @*state]
    (c/render game
      (-> entity
          (assoc
            :clear {:color [1 1 1 1] :depth 1}
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project 0 (eu/get-width game) (eu/get-height game) 0 400 -400)
          (t/translate tx ty 0)
          (t/rotate (m/deg->rad 40) :x)
          (t/rotate (m/deg->rad 25) :y)
          (t/rotate (m/deg->rad 325) :z)
          (t/scale rx ry 1))))
  game)

(defn scale-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :rx 1 :ry 1})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample play-cljc.gl.examples-3d/scale-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/scale-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-3d/scale-3d-render)))

;; perspective-3d

(defn perspective-3d-render [{:keys [entity *state] :as game}]
  (eu/resize-example game)
  (let [{:keys [cx cy]} @*state]
    (c/render game
      (-> entity
          (assoc
            :clear {:color [1 1 1 1] :depth 1}
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project (m/deg->rad 60) (/ (eu/get-width game)(eu/get-height game)) 1 2000)
          (t/translate cx cy -150)
          (t/rotate (m/deg->rad 180) :x)
          (t/rotate 0 :y)
          (t/rotate 0 :z))))
  game)

(defn perspective-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        *state (atom {:cx 0 :cy 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample play-cljc.gl.examples-3d/perspective-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/perspective-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-3d/perspective-3d-render)))

;; perspective-camera-3d

(defn perspective-camera-3d-render [{:keys [entity *state] :as game}]
  (eu/resize-example game)
  (c/render game
    {:clear {:color [1 1 1 1] :depth 1}})
  (let [{:keys [cr]} @*state
        radius 200
        num-fs 5
        camera (-> (e/->camera)
                   (t/rotate cr :y)
                   (t/translate 0 0 (* radius 1.5)))
        entity (-> entity
                   (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
                   (t/project (m/deg->rad 60) (/ (eu/get-width game)(eu/get-height game)) 1 2000)
                   (t/camera camera))]
    (dotimes [i num-fs]
      (let [angle (/ (* i (math PI) 2) num-fs)
            x (* (math cos angle) radius)
            z (* (math sin angle) radius)]
        (c/render game
          (t/translate entity x 0 z)))))
  game)

(defn perspective-camera-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game (transform-f-data data/f-3d))
        *state (atom {:cr 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample play-cljc.gl.examples-3d/perspective-camera-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/perspective-camera-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-3d/perspective-camera-3d-render)))

;; perspective-camera-target-3d

(defn perspective-camera-target-3d-render [{:keys [entity *state] :as game}]
  (eu/resize-example game)
  (c/render game
    {:clear {:color [1 1 1 1] :depth 1}})
  (let [{:keys [cr]} @*state
        radius 200
        num-fs 5
        camera (-> (e/->camera)
                   (t/rotate cr :y)
                   (t/translate 0 0 (* radius 1.5))
                   (t/look-at [radius 0 0] [0 1 0]))
        entity (-> entity
                   (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
                   (t/project (m/deg->rad 60) (/ (eu/get-width game)(eu/get-height game)) 1 2000)
                   (t/camera camera))]
    (dotimes [i num-fs]
      (let [angle (/ (* i (math PI) 2) num-fs)
            x (* (math cos angle) radius)
            z (* (math sin angle) radius)]
        (c/render game
          (t/translate entity x 0 z)))))
  game)

(defn perspective-camera-target-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game (transform-f-data data/f-3d))
        *state (atom {:cr 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample play-cljc.gl.examples-3d/perspective-camera-target-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/perspective-camera-target-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-3d/perspective-camera-target-3d-render)))

;; perspective-animation-3d

(defn perspective-animation-3d-render [{:keys [entity state delta-time] :as game}]
  (eu/resize-example game)
  (c/render game
    (-> entity
        (assoc
          :clear {:color [1 1 1 1] :depth 1}
          :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
        (t/project (m/deg->rad 60) (/ (eu/get-width game)(eu/get-height game)) 1 2000)
        (t/translate 0 0 -360)
        (t/rotate (:rx state) :x)
        (t/rotate (:ry state) :y)
        (t/rotate (:rz state) :z)))
  (update-in game [:state :ry] + (* 1.2 delta-time)))

(defn perspective-animation-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)
               :rz (m/deg->rad 320)}]
    (assoc game :entity entity :state state)))

(defexample play-cljc.gl.examples-3d/perspective-animation-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/perspective-animation-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-3d/perspective-animation-3d-render)))

;; perspective-texture-3d

(defn perspective-texture-3d-render [{:keys [entity state delta-time] :as game}]
  (eu/resize-example game)
  (let [camera (-> (e/->camera)
                   (t/translate 0 0 200)
                   (t/look-at [0 0 0] [0 1 0]))]
    (c/render game
      (-> entity
          (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project (m/deg->rad 60) (/ (eu/get-width game)(eu/get-height game)) 1 2000)
          (t/camera camera)
          (t/rotate (:rx state) :x)
          (t/rotate (:ry state) :y))))
  (-> game
      (update-in [:state :rx] + (* 1.2 delta-time))
      (update-in [:state :ry] + (* 0.7 delta-time))))

(defn perspective-texture-3d-init [game {:keys [data width height]}]
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

(defexample play-cljc.gl.examples-3d/perspective-texture-3d
  {:with-card card}
  (let [game (play-cljc.gl.example-utils/init-example card)]
    (play-cljc.gl.example-utils/get-image "f-texture.png"
      (fn [image]
        (->> (play-cljc.gl.examples-3d/perspective-texture-3d-init game image)
             (play-cljc.gl.example-utils/game-loop
               play-cljc.gl.examples-3d/perspective-texture-3d-render))))))

;; perspective-texture-data-3d

(defn perspective-texture-data-3d-render [{:keys [entity state delta-time] :as game}]
  (eu/resize-example game)
  (let [camera (-> (e/->camera)
                   (t/translate 0 0 2)
                   (t/look-at [0 0 0] [0 1 0]))]
    (c/render game
      (-> entity
          (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project (m/deg->rad 60) (/ (eu/get-width game)(eu/get-height game)) 1 2000)
          (t/camera camera)
          (t/rotate (:rx state) :x)
          (t/rotate (:ry state) :y))))
  (-> game
      (update-in [:state :rx] + (* 1.2 delta-time))
      (update-in [:state :ry] + (* 0.7 delta-time))))

(defn perspective-texture-data-3d-init [game]
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

(defexample play-cljc.gl.examples-3d/perspective-texture-data-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/perspective-texture-data-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-3d/perspective-texture-data-3d-render)))

;; perspective-texture-meta-3d

(def target-width 256)
(def target-height 256)

(defn render-cube [entity {:keys [rx ry]} aspect]
  (let [camera (-> (e/->camera)
                   (t/translate 0 0 2)
                   (t/look-at [0 0 0] [0 1 0]))]
    (-> entity
        (t/project (m/deg->rad 60) aspect 1 2000)
        (t/camera camera)
        (t/rotate rx :x)
        (t/rotate ry :y))))

(defn perspective-texture-meta-3d-render [{:keys [entity inner-entity state delta-time] :as game}]
  (eu/resize-example game)
  (c/render game
    (-> entity
        (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
        (render-cube state (/ (eu/get-width game) (eu/get-height game)))
        (assoc :render-to-texture {'u_texture
                                   [(-> inner-entity
                                        (assoc :viewport {:x 0 :y 0
                                                          :width target-width
                                                          :height target-height})
                                        (render-cube state (/ target-width target-height)))]})))
  (-> game
      (update-in [:state :rx] + (* 1.2 delta-time))
      (update-in [:state :ry] + (* 0.7 delta-time))))

(defn perspective-texture-meta-3d-init [game]
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

(defexample play-cljc.gl.examples-3d/perspective-texture-meta-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-3d/perspective-texture-meta-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-3d/perspective-texture-meta-3d-render)))

