(ns play-cljc.examples-3d
  (:require [play-cljc.core :as c]
            [play-cljc.utils :as u]
            [play-cljc.example-utils :as eu]
            [play-cljc.example-data :as data]
            [play-cljc.math :as m]
            [play-cljc.macros-js :refer-macros [gl]])
  (:require-macros [dynadoc.example :refer [defexample]]))

(defn f-entity [game f-data]
  (c/create-entity game
    {:vertex data/three-d-vertex-shader
     :fragment data/three-d-fragment-shader
     :attributes {'a_position {:data f-data
                               :type (gl game FLOAT)
                               :size 3}
                  'a_color {:data data/f-3d-colors
                            :type (gl game UNSIGNED_BYTE)
                            :size 3
                            :normalize true}}}))

(defn transform-f-data [f-data]
  (let [matrix (m/multiply-matrices 4
                 (m/translation-matrix-3d -50 -75 -15)
                 (m/x-rotation-matrix-3d js/Math.PI))]
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

(defn translation-3d-render [game entity {:keys [x y]}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
      :uniforms {'u_matrix
                 (->> (m/ortho-matrix-3d {:left 0
                                          :right (u/get-width game)
                                          :bottom (u/get-height game)
                                          :top 0
                                          :near 400
                                          :far -400})
                      (m/multiply-matrices 4 (m/translation-matrix-3d x y 0))
                      (m/multiply-matrices 4 (m/x-rotation-matrix-3d (m/deg->rad 40)))
                      (m/multiply-matrices 4 (m/y-rotation-matrix-3d (m/deg->rad 25)))
                      (m/multiply-matrices 4 (m/z-rotation-matrix-3d (m/deg->rad 325))))})))

(defn translation-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        *state (atom {:x 0 :y 0})]
    (eu/listen-for-mouse game #(translation-3d-render game entity (swap! *state merge %)))
    (translation-3d-render game entity @*state)))

(defexample play-cljc.examples-3d/translation-3d
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-3d/translation-3d-init)))

;; rotation-3d

(defn rotation-3d-render [game entity {:keys [tx ty r]}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
      :uniforms {'u_matrix
                 (->> (m/ortho-matrix-3d {:left 0
                                          :right (u/get-width game)
                                          :bottom (u/get-height game)
                                          :top 0
                                          :near 400
                                          :far -400})
                      (m/multiply-matrices 4 (m/translation-matrix-3d tx ty 0))
                      (m/multiply-matrices 4 (m/x-rotation-matrix-3d r))
                      (m/multiply-matrices 4 (m/y-rotation-matrix-3d r))
                      (m/multiply-matrices 4 (m/z-rotation-matrix-3d r))
                      ;; make it rotate around its center
                      (m/multiply-matrices 4 (m/translation-matrix-3d -50 -75 0)))})))

(defn rotation-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse (merge game @*state)
      #(rotation-3d-render game entity (swap! *state merge %)))
    (rotation-3d-render game entity @*state)))

(defexample play-cljc.examples-3d/rotation-3d
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-3d/rotation-3d-init)))

;; scale-3d

(defn scale-3d-render [game entity {:keys [tx ty rx ry]}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
      :uniforms {'u_matrix
                 (->> (m/ortho-matrix-3d {:left 0
                                          :right (u/get-width game)
                                          :bottom (u/get-height game)
                                          :top 0
                                          :near 400
                                          :far -400})
                      (m/multiply-matrices 4 (m/translation-matrix-3d tx ty 0))
                      (m/multiply-matrices 4 (m/x-rotation-matrix-3d (m/deg->rad 40)))
                      (m/multiply-matrices 4 (m/y-rotation-matrix-3d (m/deg->rad 25)))
                      (m/multiply-matrices 4 (m/z-rotation-matrix-3d (m/deg->rad 325)))
                      (m/multiply-matrices 4 (m/scaling-matrix-3d rx ry 1)))})))

(defn scale-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :rx 1 :ry 1})]
    (eu/listen-for-mouse (merge game @*state)
      #(scale-3d-render game entity (swap! *state merge %)))
    (scale-3d-render game entity @*state)))

(defexample play-cljc.examples-3d/scale-3d
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-3d/scale-3d-init)))

;; perspective-3d

(defn perspective-3d-render [game entity {:keys [cx cy]}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
      :uniforms {'u_matrix
                 (->> (m/perspective-matrix-3d {:field-of-view (m/deg->rad 60)
                                                :aspect (/ (u/get-width game)
                                                           (u/get-height game))
                                                :near 1
                                                :far 2000})
                      (m/multiply-matrices 4 (m/translation-matrix-3d cx cy -150))
                      (m/multiply-matrices 4 (m/x-rotation-matrix-3d (m/deg->rad 180)))
                      (m/multiply-matrices 4 (m/y-rotation-matrix-3d 0))
                      (m/multiply-matrices 4 (m/z-rotation-matrix-3d 0)))})))

(defn perspective-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        *state (atom {:cx 0 :cy 0})]
    (eu/listen-for-mouse game #(perspective-3d-render game entity (swap! *state merge %)))
    (perspective-3d-render game entity @*state)))

(defexample play-cljc.examples-3d/perspective-3d
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-3d/perspective-3d-init)))

;; perspective-camera-3d

(defn perspective-camera-3d-render [game entity {:keys [cr]}]
  (eu/resize-example game)
  (let [radius 200
        num-fs 5
        projection-matrix (m/perspective-matrix-3d {:field-of-view (m/deg->rad 60)
                                                    :aspect (/ (u/get-width game)
                                                               (u/get-height game))
                                                    :near 1
                                                    :far 2000})
        camera-matrix (->> (m/y-rotation-matrix-3d cr)
                           (m/multiply-matrices 4
                             (m/translation-matrix-3d 0 0 (* radius 1.5))))
        view-matrix (m/inverse-matrix 4 camera-matrix)
        view-projection-matrix (m/multiply-matrices 4 view-matrix projection-matrix)]
    (dotimes [i num-fs]
      (let [angle (/ (* i js/Math.PI 2) num-fs)
            x (* (js/Math.cos angle) radius)
            z (* (js/Math.sin angle) radius)
            matrix (m/multiply-matrices 4
                     (m/translation-matrix-3d x 0 z)
                     view-projection-matrix)]
        (c/render-entity game
          (assoc entity
            :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
            :uniforms {'u_matrix matrix}))))))

(defn perspective-camera-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game (transform-f-data data/f-3d))
        *state (atom {:r 0})]
    (eu/listen-for-mouse game
      #(perspective-camera-3d-render game entity (swap! *state merge %)))
    (perspective-camera-3d-render game entity @*state)))

(defexample play-cljc.examples-3d/perspective-camera-3d
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-3d/perspective-camera-3d-init)))

;; perspective-camera-target-3d

(defn perspective-camera-target-3d-render [game entity {:keys [cr]}]
  (eu/resize-example game)
  (let [radius 200
        num-fs 5
        projection-matrix (m/perspective-matrix-3d {:field-of-view (m/deg->rad 60)
                                                    :aspect (/ (u/get-width game)
                                                               (u/get-height game))
                                                    :near 1
                                                    :far 2000})
        camera-matrix (->> (m/y-rotation-matrix-3d cr)
                           (m/multiply-matrices 4
                             (m/translation-matrix-3d 0 50 (* radius 1.5))))
        camera-pos [(nth camera-matrix 12)
                    (nth camera-matrix 13)
                    (nth camera-matrix 14)]
        f-pos [radius 0 0]
        up [0 1 0]
        camera-matrix (m/look-at camera-pos f-pos up)
        view-matrix (m/inverse-matrix 4 camera-matrix)
        view-projection-matrix (m/multiply-matrices 4 view-matrix projection-matrix)]
    (dotimes [i num-fs]
      (let [angle (/ (* i js/Math.PI 2) num-fs)
            x (* (js/Math.cos angle) radius)
            z (* (js/Math.sin angle) radius)
            matrix (m/multiply-matrices 4
                     (m/translation-matrix-3d x 0 z)
                     view-projection-matrix)]
        (c/render-entity game
          (assoc entity
            :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
            :uniforms {'u_matrix matrix}))))))

(defn perspective-camera-target-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game (transform-f-data data/f-3d))
        *state (atom {:r 0})]
    (eu/listen-for-mouse game
      #(perspective-camera-target-3d-render game entity (swap! *state merge %)))
    (perspective-camera-target-3d-render game entity @*state)))

(defexample play-cljc.examples-3d/perspective-camera-target-3d
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-3d/perspective-camera-target-3d-init)))

;; perspective-animation-3d

(defn perspective-animation-3d-render [game entity {:keys [rx ry rz then now] :as state}]
  (eu/resize-example game)
  (c/render-entity game
    (assoc entity
      :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
      :uniforms {'u_matrix
                 (->> (m/perspective-matrix-3d {:field-of-view (m/deg->rad 60)
                                                :aspect (/ (u/get-width game)
                                                           (u/get-height game))
                                                :near 1
                                                :far 2000})
                      (m/multiply-matrices 4 (m/translation-matrix-3d 0 0 -360))
                      (m/multiply-matrices 4 (m/x-rotation-matrix-3d rx))
                      (m/multiply-matrices 4 (m/y-rotation-matrix-3d ry))
                      (m/multiply-matrices 4 (m/z-rotation-matrix-3d rz)))}))
  (js/requestAnimationFrame #(perspective-animation-3d-render game entity
                               (-> state
                                   (update :ry + (* 1.2 (- now then)))
                                   (assoc :then now :now (* % 0.001))))))

(defn perspective-animation-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (f-entity game data/f-3d)
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)
               :rz (m/deg->rad 320)
               :then 0
               :now 0}]
    (perspective-animation-3d-render game entity state)))

(defexample play-cljc.examples-3d/perspective-animation-3d
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-3d/perspective-animation-3d-init)))

;; perspective-texture-3d

(defn perspective-texture-3d-render [game entity {:keys [rx ry then now] :as state}]
  (eu/resize-example game)
  (let [projection-matrix (m/perspective-matrix-3d {:field-of-view (m/deg->rad 60)
                                                    :aspect (/ (u/get-width game)
                                                               (u/get-height game))
                                                    :near 1
                                                    :far 2000})
        camera-pos [0 0 200]
        target [0 0 0]
        up [0 1 0]
        camera-matrix (m/look-at camera-pos target up)
        view-matrix (m/inverse-matrix 4 camera-matrix)
        view-projection-matrix (m/multiply-matrices 4 view-matrix projection-matrix)]
    (c/render-entity game
      (assoc entity
        :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
        :uniforms {'u_matrix
                   (->> view-projection-matrix
                        (m/multiply-matrices 4 (m/x-rotation-matrix-3d rx))
                        (m/multiply-matrices 4 (m/y-rotation-matrix-3d ry)))}))
    (js/requestAnimationFrame #(perspective-texture-3d-render game entity
                                 (-> state
                                     (update :rx + (* 1.2 (- now then)))
                                     (update :ry + (* 0.7 (- now then)))
                                     (assoc :then now :now (* % 0.001)))))))

(defn perspective-texture-3d-init [game {:keys [image]}]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (c/create-entity game
                 {:vertex data/texture-vertex-shader
                  :fragment data/texture-fragment-shader
                  :attributes {'a_position {:data (transform-f-data data/f-3d)
                                            :type (gl game FLOAT)
                                            :size 3}
                               'a_texcoord {:data data/f-texcoords
                                            :type (gl game FLOAT)
                                            :size 2
                                            :normalize true}}
                  :uniforms {'u_texture {:data image
                                         :opts {:mip-level 0
                                                :internal-fmt (gl game RGBA)
                                                :src-fmt (gl game RGBA)
                                                :src-type (gl game UNSIGNED_BYTE)}
                                         :mipmap true}}
                  :clear {:color [0 0 0 0] :depth 1}})
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)
               :then 0
               :now 0}]
    (perspective-texture-3d-render game entity state)))

(defn perspective-texture-3d-load [game]
  (eu/get-image "f-texture.png" (partial perspective-texture-3d-init game)))

(defexample play-cljc.examples-3d/perspective-texture-3d
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-3d/perspective-texture-3d-load)))

;; perspective-texture-data-3d

(defn perspective-texture-data-3d-render [game entity {:keys [rx ry then now] :as state}]
  (eu/resize-example game)
  (let [projection-matrix (m/perspective-matrix-3d {:field-of-view (m/deg->rad 60)
                                                    :aspect (/ (u/get-width game)
                                                               (u/get-height game))
                                                    :near 1
                                                    :far 2000})
        camera-pos [0 0 2]
        target [0 0 0]
        up [0 1 0]
        camera-matrix (m/look-at camera-pos target up)
        view-matrix (m/inverse-matrix 4 camera-matrix)
        view-projection-matrix (m/multiply-matrices 4 view-matrix projection-matrix)]
    (c/render-entity game
      (assoc entity
        :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
        :uniforms {'u_matrix
                   (->> view-projection-matrix
                        (m/multiply-matrices 4 (m/x-rotation-matrix-3d rx))
                        (m/multiply-matrices 4 (m/y-rotation-matrix-3d ry)))}))
    (js/requestAnimationFrame #(perspective-texture-data-3d-render game entity
                                 (-> state
                                     (update :rx + (* 1.2 (- now then)))
                                     (update :ry + (* 0.7 (- now then)))
                                     (assoc :then now :now (* % 0.001)))))))

(defn perspective-texture-data-3d-init [game]
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
                  :clear {:color [0 0 0 0] :depth 1}})
        state {:rx (m/deg->rad 190)
               :ry (m/deg->rad 40)
               :then 0
               :now 0}]
    (perspective-texture-data-3d-render game entity state)))

(defexample play-cljc.examples-3d/perspective-texture-data-3d
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-3d/perspective-texture-data-3d-init)))

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

(defn perspective-texture-meta-3d-render [game entities {:keys [then now] :as state}]
  (eu/resize-example game)
  (let [[inner-entity entity] entities]
    (c/render-entity game
      (assoc entity
        :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
        :uniforms {'u_matrix (cube state (/ (u/get-width game) (u/get-height game)))}
        :render-to-texture {'u_texture (assoc inner-entity
                                         :viewport {:x 0 :y 0 :width target-width :height target-height}
                                         :uniforms {'u_matrix (cube state (/ target-width target-height))})})))
  (js/requestAnimationFrame #(perspective-texture-meta-3d-render game entities
                               (-> state
                                   (update :rx + (* 1.2 (- now then)))
                                   (update :ry + (* 0.7 (- now then)))
                                   (assoc :then now :now (* % 0.001))))))

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
    (perspective-texture-meta-3d-render game [inner-entity entity] state)))

(defexample play-cljc.examples-3d/perspective-texture-meta-3d
  {:with-card card}
  (->> (play-cljc.example-utils/init-example card)
       (play-cljc.examples-3d/perspective-texture-meta-3d-init)))

