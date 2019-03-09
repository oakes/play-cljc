(ns play-cljc.examples-3d
  (:require [play-cljc.core :as c]
            [play-cljc.utils :as u]
            [play-cljc.example-utils :as eu]
            [play-cljc.example-data :as data]
            [play-cljc.math :as m]
            [play-cljc.transforms :as t]
            #?(:clj  [play-cljc.macros-java :refer [gl math]]
               :cljs [play-cljc.macros-js :refer-macros [gl math]])
            #?(:clj [dynadoc.example :refer [defexample]]))
  #?(:cljs (:require-macros [dynadoc.example :refer [defexample]])))

(defrecord ThreeDEntity [])

(extend-type ThreeDEntity
  t/IProject
  (project [entity attrs]
    (let [matrix (cond
                   (every? attrs [:left :right :bottom :top :near :far])
                   (m/ortho-matrix-3d attrs)
                   (every? attrs [:field-of-view :aspect :near :far])
                   (m/perspective-matrix-3d attrs)
                   :else
                   (throw (ex-info "Can't project entity" entity)))]
      (update-in entity [:uniforms 'u_matrix]
        #(m/multiply-matrices 4 matrix %))))
  t/ITranslate
  (translate [entity {:keys [x y z]}]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 4 (m/translation-matrix-3d x y z) %)))
  t/IScale
  (scale [entity {:keys [x y z]}]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 4 (m/scaling-matrix-3d x y z) %)))
  t/IRotate
  (rotate [entity {:keys [angle axis]}]
    (let [matrix (case axis
                   :x (m/x-rotation-matrix-3d angle)
                   :y (m/y-rotation-matrix-3d angle)
                   :z (m/z-rotation-matrix-3d angle))]
      (update-in entity [:uniforms 'u_matrix]
        #(m/multiply-matrices 4 matrix %))))
  t/ICamera
  (camera [entity {:keys [matrix]}]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 4 (m/inverse-matrix 4 matrix) %))))

(defrecord Camera [])

(extend-type Camera
  t/ITranslate
  (translate [camera {:keys [x y z]}]
    (update camera :matrix
      #(m/multiply-matrices 4 (m/translation-matrix-3d x y z) %)))
  t/IRotate
  (rotate [camera {:keys [angle axis]}]
    (let [matrix (case axis
                   :x (m/x-rotation-matrix-3d angle)
                   :y (m/y-rotation-matrix-3d angle)
                   :z (m/z-rotation-matrix-3d angle))]
      (update camera :matrix
        #(m/multiply-matrices 4 matrix %))))
  t/ILookAt
  (look-at [{:keys [matrix] :as camera} {:keys [target up]}]
    (when-not matrix
      (throw (ex-info "Camera must have an existing matrix in order to use look-at" camera)))
    (let [camera-pos [(nth matrix 12)
                      (nth matrix 13)
                      (nth matrix 14)]]
      (assoc camera :matrix (m/look-at camera-pos target up)))))

(defn f-entity [game f-data]
  (->> {:vertex data/three-d-vertex-shader
        :fragment data/three-d-fragment-shader
        :attributes {'a_position {:data f-data
                                  :type (gl game FLOAT)
                                  :size 3}
                     'a_color {:data (mapv #(/ % 255) data/f-3d-colors)
                               :type (gl game FLOAT)
                               :size 3}}}
       (c/create-entity game)
       map->ThreeDEntity))

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

(defn translation-3d-render [game [entity *state :as state]]
  (eu/resize-example game)
  (let [{:keys [x y]} @*state]
    (c/render-entity game
      (-> entity
          (assoc
            :clear {:color [1 1 1 1] :depth 1}
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project {:left 0
                      :right (eu/get-width game)
                      :bottom (eu/get-height game)
                      :top 0
                      :near 400
                      :far -400})
          (t/translate {:x x :y y :z 0})
          (t/rotate {:angle (m/deg->rad 40) :axis :x})
          (t/rotate {:angle (m/deg->rad 25) :axis :y})
          (t/rotate {:angle (m/deg->rad 325) :axis :z}))))
  state)

(defn translation-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        *state (atom {:x 0 :y 0})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.examples-3d/translation-3d
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-3d/translation-3d-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-3d/translation-3d-render
      game state)))

;; rotation-3d

(defn rotation-3d-render [game [entity *state :as state]]
  (eu/resize-example game)
  (let [{:keys [tx ty r]} @*state]
    (c/render-entity game
      (-> entity
          (assoc
            :clear {:color [1 1 1 1] :depth 1}
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project {:left 0
                      :right (eu/get-width game)
                      :bottom (eu/get-height game)
                      :top 0
                      :near 400
                      :far -400})
          (t/translate {:x tx :y ty :z 0})
          (t/rotate {:angle r :axis :x})
          (t/rotate {:angle r :axis :y})
          (t/rotate {:angle r :axis :z})
          ;; make it rotate around its center
          (t/translate {:x -50 :y -75 :z 0}))))
  state)

(defn rotation-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.examples-3d/rotation-3d
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-3d/rotation-3d-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-3d/rotation-3d-render
      game state)))

;; scale-3d

(defn scale-3d-render [game [entity *state :as state]]
  (eu/resize-example game)
  (let [{:keys [tx ty rx ry]} @*state]
    (c/render-entity game
      (-> entity
          (assoc
            :clear {:color [1 1 1 1] :depth 1}
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project {:left 0
                      :right (eu/get-width game)
                      :bottom (eu/get-height game)
                      :top 0
                      :near 400
                      :far -400})
          (t/translate {:x tx :y ty :z 0})
          (t/rotate {:angle (m/deg->rad 40) :axis :x})
          (t/rotate {:angle (m/deg->rad 25) :axis :y})
          (t/rotate {:angle (m/deg->rad 325) :axis :z})
          (t/scale {:x rx :y ry :z 1}))))
  state)

(defn scale-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :rx 1 :ry 1})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.examples-3d/scale-3d
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-3d/scale-3d-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-3d/scale-3d-render
      game state)))

;; perspective-3d

(defn perspective-3d-render [game [entity *state :as state]]
  (eu/resize-example game)
  (let [{:keys [cx cy]} @*state]
    (c/render-entity game
      (-> entity
          (assoc
            :clear {:color [1 1 1 1] :depth 1}
            :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project {:field-of-view (m/deg->rad 60)
                      :aspect (/ (eu/get-width game)
                                 (eu/get-height game))
                      :near 1
                      :far 2000})
          (t/translate {:x cx :y cy :z -150})
          (t/rotate {:angle (m/deg->rad 180) :axis :x})
          (t/rotate {:angle 0 :axis :y})
          (t/rotate {:angle 0 :axis :z}))))
  state)

(defn perspective-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        *state (atom {:cx 0 :cy 0})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.examples-3d/perspective-3d
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-3d/perspective-3d-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-3d/perspective-3d-render
      game state)))

;; perspective-camera-3d

(defn perspective-camera-3d-render [game [entity *state :as state]]
  (eu/resize-example game)
  (c/render-entity game
    {:clear {:color [1 1 1 1] :depth 1}})
  (let [{:keys [cr]} @*state
        radius 200
        num-fs 5
        camera (-> (->Camera)
                   (t/rotate {:angle cr :axis :y})
                   (t/translate {:x 0 :y 0 :z (* radius 1.5)}))
        entity (-> entity
                   (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
                   (t/project {:field-of-view (m/deg->rad 60)
                               :aspect (/ (eu/get-width game)
                                          (eu/get-height game))
                               :near 1
                               :far 2000})
                   (t/camera camera))]
    (dotimes [i num-fs]
      (let [angle (/ (* i (math PI) 2) num-fs)
            x (* (math cos angle) radius)
            z (* (math sin angle) radius)]
        (c/render-entity game
          (t/translate entity {:x x :y 0 :z z})))))
  state)

(defn perspective-camera-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game (transform-f-data data/f-3d))
        *state (atom {:cr 0})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.examples-3d/perspective-camera-3d
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-3d/perspective-camera-3d-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-3d/perspective-camera-3d-render
      game state)))

;; perspective-camera-target-3d

(defn perspective-camera-target-3d-render [game [entity *state :as state]]
  (eu/resize-example game)
  (c/render-entity game
    {:clear {:color [1 1 1 1] :depth 1}})
  (let [{:keys [cr]} @*state
        radius 200
        num-fs 5
        camera (-> (->Camera)
                   (t/rotate {:angle cr :axis :y})
                   (t/translate {:x 0 :y 0 :z (* radius 1.5)})
                   (t/look-at {:target [radius 0 0] :up [0 1 0]}))
        entity (-> entity
                   (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
                   (t/project {:field-of-view (m/deg->rad 60)
                               :aspect (/ (eu/get-width game)
                                          (eu/get-height game))
                               :near 1
                               :far 2000})
                   (t/camera camera))]
    (dotimes [i num-fs]
      (let [angle (/ (* i (math PI) 2) num-fs)
            x (* (math cos angle) radius)
            z (* (math sin angle) radius)]
        (c/render-entity game
          (t/translate entity {:x x :y 0 :z z})))))
            
  state)

(defn perspective-camera-target-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game (transform-f-data data/f-3d))
        *state (atom {:cr 0})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.examples-3d/perspective-camera-target-3d
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-3d/perspective-camera-target-3d-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-3d/perspective-camera-target-3d-render
      game state)))

;; perspective-animation-3d

(defn perspective-animation-3d-render [game [entity {:keys [rx ry rz then now] :as state}]]
  (eu/resize-example game)
  (c/render-entity game
    (-> entity
        (assoc
          :clear {:color [1 1 1 1] :depth 1}
          :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
        (t/project {:field-of-view (m/deg->rad 60)
                    :aspect (/ (eu/get-width game)
                               (eu/get-height game))
                    :near 1
                    :far 2000})
        (t/translate {:x 0 :y 0 :z -360})
        (t/rotate {:angle rx :axis :x})
        (t/rotate {:angle ry :axis :y})
        (t/rotate {:angle rz :axis :z})))
  [entity
   (-> state
       (update :ry + (* 1.2 (- now then)))
       (assoc :then now :now (:time game)))])

(defn perspective-animation-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)
               :rz (m/deg->rad 320)
               :then 0
               :now 0}]
    [entity state]))

(defexample play-cljc.examples-3d/perspective-animation-3d
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-3d/perspective-animation-3d-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-3d/perspective-animation-3d-render
      game state)))

;; perspective-texture-3d

(defn perspective-texture-3d-render [game [entity {:keys [rx ry then now] :as state}]]
  (eu/resize-example game)
  (let [camera (-> (->Camera)
                   (t/translate {:x 0 :y 0 :z 200})
                   (t/look-at {:target [0 0 0] :up [0 1 0]}))]
    (c/render-entity game
      (-> entity
          (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project {:field-of-view (m/deg->rad 60)
                      :aspect (/ (eu/get-width game)
                                 (eu/get-height game))
                      :near 1
                      :far 2000})
          (t/camera camera)
          (t/rotate {:angle rx :axis :x})
          (t/rotate {:angle ry :axis :y})))
    [entity
     (-> state
         (update :rx + (* 1.2 (- now then)))
         (update :ry + (* 0.7 (- now then)))
         (assoc :then now :now (:time game)))]))

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
                    (c/create-entity game)
                    map->ThreeDEntity)
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)
               :then 0
               :now 0}]
    [entity state]))

(defexample play-cljc.examples-3d/perspective-texture-3d
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)]
    (play-cljc.example-utils/get-image "f-texture.png"
      (fn [image]
        (let [state (play-cljc.examples-3d/perspective-texture-3d-init game image)]
          (play-cljc.example-utils/game-loop
            play-cljc.examples-3d/perspective-texture-3d-render
            game state))))))

;; perspective-texture-data-3d

(defn perspective-texture-data-3d-render [game [entity {:keys [rx ry then now] :as state}]]
  (eu/resize-example game)
  (let [camera (-> (->Camera)
                   (t/translate {:x 0 :y 0 :z 2})
                   (t/look-at {:target [0 0 0] :up [0 1 0]}))]
    (c/render-entity game
      (-> entity
          (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project {:field-of-view (m/deg->rad 60)
                      :aspect (/ (eu/get-width game)
                                 (eu/get-height game))
                      :near 1
                      :far 2000})
          (t/camera camera)
          (t/rotate {:angle rx :axis :x})
          (t/rotate {:angle ry :axis :y})))
    [entity
     (-> state
         (update :rx + (* 1.2 (- now then)))
         (update :ry + (* 0.7 (- now then)))
         (assoc :then now :now (:time game)))]))

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
                    (c/create-entity game)
                    map->ThreeDEntity)
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)
               :then 0
               :now 0}]
    [entity state]))

(defexample play-cljc.examples-3d/perspective-texture-data-3d
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-3d/perspective-texture-data-3d-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-3d/perspective-texture-data-3d-render
      game state)))

;; perspective-texture-meta-3d

(def target-width 256)
(def target-height 256)

(defn cube [{:keys [rx ry]} aspect]
  (let [projection-matrix (m/perspective-matrix-3d {:field-of-view (m/deg->rad 60)
                                                    :aspect aspect
                                                    :near 1
                                                    :far 2000})
        camera-pos [0 0 2]
        target [0 0 0]
        up [0 1 0]
        camera-matrix (m/look-at camera-pos target up)
        view-matrix (m/inverse-matrix 4 camera-matrix)
        view-projection-matrix (m/multiply-matrices 4 view-matrix projection-matrix)]
    (->> view-projection-matrix
         (m/multiply-matrices 4 (m/x-rotation-matrix-3d rx))
         (m/multiply-matrices 4 (m/y-rotation-matrix-3d ry)))))

(defn perspective-texture-meta-3d-render [game [entities {:keys [then now] :as state}]]
  (eu/resize-example game)
  (let [[inner-entity entity] entities]
    (c/render-entity game
      (assoc entity
        :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)}
        :uniforms {'u_matrix (cube state (/ (eu/get-width game) (eu/get-height game)))}
        :render-to-texture {'u_texture
                            (assoc inner-entity
                              :viewport {:x 0 :y 0 :width target-width :height target-height}
                              :uniforms {'u_matrix (cube state (/ target-width target-height))})})))
  [entities
   (-> state
       (update :rx + (* 1.2 (- now then)))
       (update :ry + (* 0.7 (- now then)))
       (assoc :then now :now (:time game)))])

(defn perspective-texture-meta-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (c/create-entity game
                 {:vertex data/texture-vertex-shader
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
                  :clear {:color [1 1 1 1] :depth 1}})
        inner-entity (c/create-entity game
                       {:vertex data/texture-vertex-shader
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
                        :clear {:color [0 0 1 1] :depth 1}})
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)
               :then 0
               :now 0}]
    [[inner-entity entity] state]))

(defexample play-cljc.examples-3d/perspective-texture-meta-3d
  {:with-card card}
  (let [game (play-cljc.example-utils/init-example card)
        state (play-cljc.examples-3d/perspective-texture-meta-3d-init game)]
    (play-cljc.example-utils/game-loop
      play-cljc.examples-3d/perspective-texture-meta-3d-render
      game state)))

