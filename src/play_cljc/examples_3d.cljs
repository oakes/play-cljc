(ns play-cljc.examples-3d
  (:require [play-cljc.core :as c]
            [play-cljc.utils :as u]
            [play-cljc.client-utils :as cu]
            [goog.events :as events]
            [play-cljc.data :as data])
  (:require-macros [dynadoc.example :refer [defexample]]))

(defn f-entity [{:keys [gl] :as game} f-data]
  (c/create-entity
    {:vertex data/three-d-vertex-shader
     :fragment data/three-d-fragment-shader
     :attributes {'a_position {:data f-data
                               :type gl.FLOAT
                               :size 3
                               :normalize false
                               :stride 0
                               :offset 0}
                  'a_color {:data (js/Uint8Array. data/f-3d-colors)
                            :type gl.UNSIGNED_BYTE
                            :size 3
                            :normalize true
                            :stride 0
                            :offset 0}}}
    game))

(defn transform-f-data [f-data]
  (let [positions (js/Float32Array. f-data)
        matrix (u/multiply-matrices 4
                 (u/translation-matrix-3d -50 -75 -15)
                 (u/x-rotation-matrix-3d js/Math.PI))]
    (doseq [i (range 0 (.-length positions) 3)]
      (let [v (u/transform-vector matrix
                [(aget positions (+ i 0))
                 (aget positions (+ i 1))
                 (aget positions (+ i 2))
                 1])]
        (aset positions (+ i 0) (nth v 0))
        (aset positions (+ i 1) (nth v 1))
        (aset positions (+ i 2) (nth v 2))))
    positions))

;; translation-3d

(defn translation-3d-render [{:keys [gl] :as game} canvas entity {:keys [x y]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (c/render (c/map->Viewport {:x 0 :y 0 :width gl.canvas.width :height gl.canvas.height}) game)
  (c/render (c/map->Clear {:color [0 0 0 0] :depth 1}) game)
  (c/render
    (assoc entity
      :uniforms {'u_matrix
                 (->> (u/ortho-matrix-3d {:left 0
                                          :right gl.canvas.clientWidth
                                          :bottom gl.canvas.clientHeight
                                          :top 0
                                          :near 400
                                          :far -400})
                      (u/multiply-matrices 4 (u/translation-matrix-3d x y 0))
                      (u/multiply-matrices 4 (u/x-rotation-matrix-3d (u/deg->rad 40)))
                      (u/multiply-matrices 4 (u/y-rotation-matrix-3d (u/deg->rad 25)))
                      (u/multiply-matrices 4 (u/z-rotation-matrix-3d (u/deg->rad 325))))})
    game))

(defn translation-3d-init [canvas]
  (let [game (c/create-game (.getContext canvas "webgl2"))
        entity (f-entity game data/f-3d)
        *state (atom {:x 0 :y 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds))
              y (- (.-clientY event) (.-top bounds))]
          (translation-3d-render game canvas entity (swap! *state assoc :x x :y y)))))
    (translation-3d-render game canvas entity @*state)))

(defexample play-cljc.examples-3d/translation-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/translation-3d-init)))

;; rotation-3d

(defn rotation-3d-render [{:keys [gl] :as game} canvas entity {:keys [tx ty r]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (c/render (c/map->Viewport {:x 0 :y 0 :width gl.canvas.width :height gl.canvas.height}) game)
  (c/render (c/map->Clear {:color [0 0 0 0] :depth 1}) game)
  (c/render
    (assoc entity
      :uniforms {'u_matrix
                 (->> (u/ortho-matrix-3d {:left 0
                                          :right gl.canvas.clientWidth
                                          :bottom gl.canvas.clientHeight
                                          :top 0
                                          :near 400
                                          :far -400})
                      (u/multiply-matrices 4 (u/translation-matrix-3d tx ty 0))
                      (u/multiply-matrices 4 (u/x-rotation-matrix-3d r))
                      (u/multiply-matrices 4 (u/y-rotation-matrix-3d r))
                      (u/multiply-matrices 4 (u/z-rotation-matrix-3d r))
                      ;; make it rotate around its center
                      (u/multiply-matrices 4 (u/translation-matrix-3d -50 -75 0)))})
    game))

(defn rotation-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        game (c/create-game gl)
        entity (f-entity game data/f-3d)
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
          (rotation-3d-render game canvas entity (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-3d-render game canvas entity @*state)))

(defexample play-cljc.examples-3d/rotation-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/rotation-3d-init)))

;; scale-3d

(defn scale-3d-render [{:keys [gl] :as game} canvas entity {:keys [tx ty sx sy]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (c/render (c/map->Viewport {:x 0 :y 0 :width gl.canvas.width :height gl.canvas.height}) game)
  (c/render (c/map->Clear {:color [0 0 0 0] :depth 1}) game)
  (c/render
    (assoc entity
      :uniforms {'u_matrix
                 (->> (u/ortho-matrix-3d {:left 0
                                          :right gl.canvas.clientWidth
                                          :bottom gl.canvas.clientHeight
                                          :top 0
                                          :near 400
                                          :far -400})
                      (u/multiply-matrices 4 (u/translation-matrix-3d tx ty 0))
                      (u/multiply-matrices 4 (u/x-rotation-matrix-3d (u/deg->rad 40)))
                      (u/multiply-matrices 4 (u/y-rotation-matrix-3d (u/deg->rad 25)))
                      (u/multiply-matrices 4 (u/z-rotation-matrix-3d (u/deg->rad 325)))
                      (u/multiply-matrices 4 (u/scaling-matrix-3d sx sy 1)))})
    game))

(defn scale-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        game (c/create-game gl)
        entity (f-entity game data/f-3d)
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
          (scale-3d-render game canvas entity (swap! *state assoc :sx sx :sy sy)))))
    (scale-3d-render game canvas entity @*state)))

(defexample play-cljc.examples-3d/scale-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/scale-3d-init)))

;; perspective-3d

(defn perspective-3d-render [{:keys [gl] :as game} canvas entity {:keys [tx ty]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (c/render (c/map->Viewport {:x 0 :y 0 :width gl.canvas.width :height gl.canvas.height}) game)
  (c/render (c/map->Clear {:color [0 0 0 0] :depth 1}) game)
  (c/render
    (assoc entity
      :uniforms {'u_matrix
                 (->> (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                                :aspect (/ gl.canvas.clientWidth
                                                           gl.canvas.clientHeight)
                                                :near 1
                                                :far 2000})
                      (u/multiply-matrices 4 (u/translation-matrix-3d tx ty -150))
                      (u/multiply-matrices 4 (u/x-rotation-matrix-3d (u/deg->rad 180)))
                      (u/multiply-matrices 4 (u/y-rotation-matrix-3d 0))
                      (u/multiply-matrices 4 (u/z-rotation-matrix-3d 0)))})
    game))

(defn perspective-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        game (c/create-game gl)
        entity (f-entity game data/f-3d)
        *state (atom {:tx 0 :ty 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
              y (- (.-height bounds)
                   (- (.-clientY event) (.-top bounds)))]
          (perspective-3d-render game canvas entity (swap! *state assoc :tx x :ty y)))))
    (perspective-3d-render game canvas entity @*state)))

(defexample play-cljc.examples-3d/perspective-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-3d-init)))

;; perspective-camera-3d

(defn perspective-camera-3d-render [{:keys [gl] :as game} canvas entity {:keys [r]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (c/render (c/map->Viewport {:x 0 :y 0 :width gl.canvas.width :height gl.canvas.height}) game)
  (c/render (c/map->Clear {:color [0 0 0 0] :depth 1}) game)
  (let [radius 200
        num-fs 5
        projection-matrix (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                                     :aspect (/ gl.canvas.clientWidth
                                                                gl.canvas.clientHeight)
                                                     :near 1
                                                     :far 2000})
        camera-matrix (->> (u/y-rotation-matrix-3d r)
                           (u/multiply-matrices 4
                             (u/translation-matrix-3d 0 0 (* radius 1.5))))
        view-matrix (u/inverse-matrix 4 camera-matrix)
        view-projection-matrix (u/multiply-matrices 4 view-matrix projection-matrix)]
    (dotimes [i num-fs]
      (let [angle (/ (* i js/Math.PI 2) num-fs)
            x (* (js/Math.cos angle) radius)
            z (* (js/Math.sin angle) radius)
            matrix (u/multiply-matrices 4
                     (u/translation-matrix-3d x 0 z)
                     view-projection-matrix)]
        (c/render (assoc entity :uniforms {'u_matrix matrix}) game)))))

(defn perspective-camera-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        game (c/create-game gl)
        entity (f-entity game (transform-f-data data/f-3d))
        *state (atom {:r 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              r (/ (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
                   (.-width bounds))]
          (perspective-camera-3d-render game canvas entity
            (swap! *state assoc :r (-> r (* 360) u/deg->rad))))))
    (perspective-camera-3d-render game canvas entity @*state)))

(defexample play-cljc.examples-3d/perspective-camera-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-camera-3d-init)))

;; perspective-camera-target-3d

(defn perspective-camera-target-3d-render [{:keys [gl] :as game} canvas entity {:keys [r]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (c/render (c/map->Viewport {:x 0 :y 0 :width gl.canvas.width :height gl.canvas.height}) game)
  (c/render (c/map->Clear {:color [0 0 0 0] :depth 1}) game)
  (let [radius 200
        num-fs 5
        projection-matrix (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                                    :aspect (/ gl.canvas.clientWidth
                                                               gl.canvas.clientHeight)
                                                    :near 1
                                                    :far 2000})
        camera-matrix (->> (u/y-rotation-matrix-3d r)
                           (u/multiply-matrices 4
                             (u/translation-matrix-3d 0 50 (* radius 1.5))))
        camera-pos [(nth camera-matrix 12)
                    (nth camera-matrix 13)
                    (nth camera-matrix 14)]
        f-pos [radius 0 0]
        up [0 1 0]
        camera-matrix (u/look-at camera-pos f-pos up)
        view-matrix (u/inverse-matrix 4 camera-matrix)
        view-projection-matrix (u/multiply-matrices 4 view-matrix projection-matrix)]
    (dotimes [i num-fs]
      (let [angle (/ (* i js/Math.PI 2) num-fs)
            x (* (js/Math.cos angle) radius)
            z (* (js/Math.sin angle) radius)
            matrix (u/multiply-matrices 4
                     (u/translation-matrix-3d x 0 z)
                     view-projection-matrix)]
        (c/render (assoc entity :uniforms {'u_matrix matrix}) game)))))

(defn perspective-camera-target-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        game (c/create-game gl)
        entity (f-entity game (transform-f-data data/f-3d))
        *state (atom {:r 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              r (/ (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
                   (.-width bounds))]
          (perspective-camera-target-3d-render game canvas entity
            (swap! *state assoc :r (-> r (* 360) u/deg->rad))))))
    (perspective-camera-target-3d-render game canvas entity @*state)))

(defexample play-cljc.examples-3d/perspective-camera-target-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-camera-target-3d-init)))

;; perspective-animation-3d

(defn perspective-animation-3d-render [{:keys [gl] :as game} canvas entity {:keys [rx ry rz then now] :as state}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (c/render (c/map->Viewport {:x 0 :y 0 :width gl.canvas.width :height gl.canvas.height}) game)
  (c/render (c/map->Clear {:color [0 0 0 0] :depth 1}) game)
  (c/render
    (assoc entity
      :uniforms {'u_matrix
                 (->> (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                                :aspect (/ gl.canvas.clientWidth
                                                           gl.canvas.clientHeight)
                                                :near 1
                                                :far 2000})
                      (u/multiply-matrices 4 (u/translation-matrix-3d 0 0 -360))
                      (u/multiply-matrices 4 (u/x-rotation-matrix-3d rx))
                      (u/multiply-matrices 4 (u/y-rotation-matrix-3d ry))
                      (u/multiply-matrices 4 (u/z-rotation-matrix-3d rz)))})
    game)
  (js/requestAnimationFrame #(perspective-animation-3d-render game canvas entity
                               (-> state
                                   (update :ry + (* 1.2 (- now then)))
                                   (assoc :then now :now (* % 0.001))))))

(defn perspective-animation-3d-init [canvas]
  (let [game (c/create-game (.getContext canvas "webgl2"))
        entity (f-entity game data/f-3d)
        state {:rx (u/deg->rad 190)
               :ry (u/deg->rad 40)
               :rz (u/deg->rad 320)
               :then 0
               :now 0}]
    (perspective-animation-3d-render game canvas entity state)))

(defexample play-cljc.examples-3d/perspective-animation-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-animation-3d-init)))

;; perspective-texture-3d

(defn perspective-texture-3d-render [{:keys [gl] :as game} canvas entity {:keys [rx ry then now] :as state}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (c/render (c/map->Viewport {:x 0 :y 0 :width gl.canvas.width :height gl.canvas.height}) game)
  (c/render (c/map->Clear {:color [0 0 0 0] :depth 1}) game)
  (let [projection-matrix (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                                    :aspect (/ gl.canvas.clientWidth
                                                               gl.canvas.clientHeight)
                                                    :near 1
                                                    :far 2000})
        camera-pos [0 0 200]
        target [0 0 0]
        up [0 1 0]
        camera-matrix (u/look-at camera-pos target up)
        view-matrix (u/inverse-matrix 4 camera-matrix)
        view-projection-matrix (u/multiply-matrices 4 view-matrix projection-matrix)]
    (c/render
      (assoc entity
        :uniforms {'u_matrix
                   (->> view-projection-matrix
                        (u/multiply-matrices 4 (u/x-rotation-matrix-3d rx))
                        (u/multiply-matrices 4 (u/y-rotation-matrix-3d ry)))})
      game)
    (js/requestAnimationFrame #(perspective-texture-3d-render game canvas entity
                                 (-> state
                                     (update :rx + (* 1.2 (- now then)))
                                     (update :ry + (* 0.7 (- now then)))
                                     (assoc :then now :now (* % 0.001)))))))

(defn perspective-texture-3d-init [canvas image]
  (let [gl (.getContext canvas "webgl2")
        game (c/create-game gl)
        entity (c/create-entity
                 {:vertex data/texture-vertex-shader
                  :fragment data/texture-fragment-shader
                  :attributes {'a_position {:data (transform-f-data data/f-3d)
                                            :type gl.FLOAT
                                            :size 3
                                            :normalize false
                                            :stride 0
                                            :offset 0}
                               'a_texcoord {:data data/f-texcoords
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize true
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_texture {:data image
                                         :opts {:mip-level 0
                                                :internal-fmt gl.RGBA
                                                :src-fmt gl.RGBA
                                                :src-type gl.UNSIGNED_BYTE}
                                         :mipmap true}}}
                 game)
        state {:rx (u/deg->rad 190)
               :ry (u/deg->rad 40)
               :then 0
               :now 0}]
    (perspective-texture-3d-render game canvas entity state)))

(defn perspective-texture-3d-load [canvas]
  (let [image (js/Image.)]
    (doto image
      (-> .-src (set! "f-texture.png"))
      (-> .-onload (set! (fn []
                           (perspective-texture-3d-init canvas image)))))))

(defexample play-cljc.examples-3d/perspective-texture-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-texture-3d-load)))

;; perspective-texture-data-3d

(defn perspective-texture-data-3d-render [{:keys [gl] :as game} canvas entity {:keys [rx ry then now] :as state}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (c/render (c/map->Viewport {:x 0 :y 0 :width gl.canvas.width :height gl.canvas.height}) game)
  (c/render (c/map->Clear {:color [0 0 0 0] :depth 1}) game)
  (let [projection-matrix (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                                    :aspect (/ gl.canvas.clientWidth
                                                               gl.canvas.clientHeight)
                                                    :near 1
                                                    :far 2000})
        camera-pos [0 0 2]
        target [0 0 0]
        up [0 1 0]
        camera-matrix (u/look-at camera-pos target up)
        view-matrix (u/inverse-matrix 4 camera-matrix)
        view-projection-matrix (u/multiply-matrices 4 view-matrix projection-matrix)]
    (c/render
      (assoc entity
        :uniforms {'u_matrix
                   (->> view-projection-matrix
                        (u/multiply-matrices 4 (u/x-rotation-matrix-3d rx))
                        (u/multiply-matrices 4 (u/y-rotation-matrix-3d ry)))})
      game)
    (js/requestAnimationFrame #(perspective-texture-data-3d-render game canvas entity
                                 (-> state
                                     (update :rx + (* 1.2 (- now then)))
                                     (update :ry + (* 0.7 (- now then)))
                                     (assoc :then now :now (* % 0.001)))))))

(defn perspective-texture-data-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        game (c/create-game gl)
        entity (c/create-entity
                 {:vertex data/texture-vertex-shader
                  :fragment data/texture-fragment-shader
                  :attributes {'a_position {:data data/cube
                                            :type gl.FLOAT
                                            :size 3
                                            :normalize false
                                            :stride 0
                                            :offset 0}
                               'a_texcoord {:data data/cube-texcoords
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize true
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_texture {:data (js/Uint8Array. [128 64 128 0 192 0])
                                         :opts {:mip-level 0
                                                :internal-fmt gl.R8
                                                :width 3
                                                :height 2
                                                :border 0
                                                :src-fmt gl.RED
                                                :src-type gl.UNSIGNED_BYTE}
                                         :alignment 1
                                         :params {gl.TEXTURE_MIN_FILTER gl.NEAREST
                                                  gl.TEXTURE_MAG_FILTER gl.NEAREST
                                                  gl.TEXTURE_WRAP_S gl.CLAMP_TO_EDGE
                                                  gl.TEXTURE_WRAP_T gl.CLAMP_TO_EDGE}}}}
                 game)
        state {:rx (u/deg->rad 190)
               :ry (u/deg->rad 40)
               :then 0
               :now 0}]
    (perspective-texture-data-3d-render game canvas entity state)))

(defexample play-cljc.examples-3d/perspective-texture-data-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-texture-data-3d-init)))

;; perspective-texture-meta-3d

(def target-width 256)
(def target-height 256)

(defn draw-cube [game entity {:keys [rx ry]} aspect]
  (let [projection-matrix (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                                    :aspect aspect
                                                    :near 1
                                                    :far 2000})
        camera-pos [0 0 2]
        target [0 0 0]
        up [0 1 0]
        camera-matrix (u/look-at camera-pos target up)
        view-matrix (u/inverse-matrix 4 camera-matrix)
        view-projection-matrix (u/multiply-matrices 4 view-matrix projection-matrix)]
    (c/render
      (assoc entity
        :uniforms {'u_matrix
                   (->> view-projection-matrix
                      (u/multiply-matrices 4 (u/x-rotation-matrix-3d rx))
                      (u/multiply-matrices 4 (u/y-rotation-matrix-3d ry)))})
      game)))

(defn perspective-texture-meta-3d-render [{:keys [gl] :as game} canvas entities {:keys [then now] :as state}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (let [[inner-entity entity] entities]
    (.bindFramebuffer gl gl.FRAMEBUFFER (-> entity :textures (get 'u_texture) :framebuffer))
    (c/render (c/map->Viewport {:x 0 :y 0 :width target-width :height target-height}) game)
    (c/render (c/map->Clear {:color [0 0 1 1] :depth 1}) game)
    (draw-cube game inner-entity state (/ target-width target-height))
    (.bindFramebuffer gl gl.FRAMEBUFFER nil)
    (c/render (c/map->Viewport {:x 0 :y 0 :width gl.canvas.clientWidth :height gl.canvas.clientHeight}) game)
    (c/render (c/map->Clear {:color [1 1 1 1] :depth 1}) game)
    (draw-cube game entity state (/ gl.canvas.clientWidth gl.canvas.clientHeight)))
  (js/requestAnimationFrame #(perspective-texture-meta-3d-render game canvas entities
                               (-> state
                                   (update :rx + (* 1.2 (- now then)))
                                   (update :ry + (* 0.7 (- now then)))
                                   (assoc :then now :now (* % 0.001))))))

(defn perspective-texture-meta-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        game (c/create-game gl)
        entity (c/create-entity
                 {:vertex data/texture-vertex-shader
                  :fragment data/texture-fragment-shader
                  :attributes {'a_position {:data data/cube
                                            :type gl.FLOAT
                                            :size 3
                                            :normalize false
                                            :stride 0
                                            :offset 0}
                               'a_texcoord {:data data/cube-texcoords
                                            :type gl.FLOAT
                                            :size 2
                                            :normalize true
                                            :stride 0
                                            :offset 0}}
                  :uniforms {'u_texture {:data nil
                                         :opts {:mip-level 0
                                                :internal-fmt gl.RGBA
                                                :width target-width
                                                :height target-height
                                                :border 0
                                                :src-fmt gl.RGBA
                                                :src-type gl.UNSIGNED_BYTE}
                                         :params {gl.TEXTURE_MIN_FILTER gl.LINEAR
                                                  gl.TEXTURE_WRAP_S gl.CLAMP_TO_EDGE
                                                  gl.TEXTURE_WRAP_T gl.CLAMP_TO_EDGE}}}}
                 game)
        inner-entity (c/create-entity
                       {:vertex data/texture-vertex-shader
                        :fragment data/texture-fragment-shader
                        :attributes {'a_position {:data data/cube
                                                  :type gl.FLOAT
                                                  :size 3
                                                  :normalize false
                                                  :stride 0
                                                  :offset 0}
                                     'a_texcoord {:data data/cube-texcoords
                                                  :type gl.FLOAT
                                                  :size 2
                                                  :normalize true
                                                  :stride 0
                                                  :offset 0}}
                        :uniforms {'u_texture {:data (js/Uint8Array. [128 64 128 0 192 0])
                                               :opts {:mip-level 0
                                                      :internal-fmt gl.R8
                                                      :width 3
                                                      :height 2
                                                      :border 0
                                                      :src-fmt gl.RED
                                                      :src-type gl.UNSIGNED_BYTE}
                                               :alignment 1
                                               :params {gl.TEXTURE_MIN_FILTER gl.NEAREST
                                                        gl.TEXTURE_MAG_FILTER gl.NEAREST
                                                        gl.TEXTURE_WRAP_S gl.CLAMP_TO_EDGE
                                                        gl.TEXTURE_WRAP_T gl.CLAMP_TO_EDGE}}}}
                       game)
        state {:rx (u/deg->rad 190)
               :ry (u/deg->rad 40)
               :then 0
               :now 0}]
    (perspective-texture-meta-3d-render game canvas [inner-entity entity] state)))

(defexample play-cljc.examples-3d/perspective-texture-meta-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-texture-meta-3d-init)))

