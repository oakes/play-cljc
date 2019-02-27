(ns play-cljc.examples-3d
  (:require [play-cljc.utils :as u]
            [play-cljc.client-utils :as cu]
            [goog.events :as events]
            [play-cljc.data :as data])
  (:require-macros [dynadoc.example :refer [defexample]]))

;; translation-3d

(defn translation-3d-render [canvas
                             {:keys [gl program vao matrix-location cnt]}
                             {:keys [x y]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
    (->> (u/ortho-matrix-3d {:left 0
                              :right gl.canvas.clientWidth
                              :bottom gl.canvas.clientHeight
                              :top 0
                              :near 400
                              :far -400})
         (u/multiply-matrices 4 (u/translation-matrix-3d x y 0))
         (u/multiply-matrices 4 (u/x-rotation-matrix-3d (u/deg->rad 40)))
         (u/multiply-matrices 4 (u/y-rotation-matrix-3d (u/deg->rad 25)))
         (u/multiply-matrices 4 (u/z-rotation-matrix-3d (u/deg->rad 325)))))
  (.drawArrays gl gl.TRIANGLES 0 cnt))

(defn translation-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_color" (js/Uint8Array. (clj->js data/f-3d-colors))
                     {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
                   (u/create-buffer gl program "a_position"
                     (js/Float32Array. data/f-3d) {:size 3}))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt @*buffers}
        *state (atom {:x 0 :y 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds))
              y (- (.-clientY event) (.-top bounds))]
          (translation-3d-render canvas props (swap! *state assoc :x x :y y)))))
    (translation-3d-render canvas props @*state)))

(defexample play-cljc.examples-3d/translation-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/translation-3d-init)))

;; rotation-3d

(defn rotation-3d-render [canvas
                          {:keys [gl program vao matrix-location cnt]}
                          {:keys [tx ty r]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
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
         (u/multiply-matrices 4 (u/translation-matrix-3d -50 -75 0))))
  (.drawArrays gl gl.TRIANGLES 0 cnt))

(defn rotation-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_color" (js/Uint8Array. (clj->js data/f-3d-colors))
                     {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
                   (u/create-buffer gl program "a_position"
                     (js/Float32Array. data/f-3d) {:size 3}))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt @*buffers}
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
          (rotation-3d-render canvas props (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-3d-render canvas props @*state)))

(defexample play-cljc.examples-3d/rotation-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/rotation-3d-init)))

;; scale-3d

(defn scale-3d-render [canvas
                       {:keys [gl program vao matrix-location cnt]}
                       {:keys [tx ty sx sy]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
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
         (u/multiply-matrices 4 (u/scaling-matrix-3d sx sy 1))))
  (.drawArrays gl gl.TRIANGLES 0 cnt))

(defn scale-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_color" (js/Uint8Array. (clj->js data/f-3d-colors))
                     {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
                   (u/create-buffer gl program "a_position"
                     (js/Float32Array. data/f-3d) {:size 3}))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt @*buffers}
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
          (scale-3d-render canvas props (swap! *state assoc :sx sx :sy sy)))))
    (scale-3d-render canvas props @*state)))

(defexample play-cljc.examples-3d/scale-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/scale-3d-init)))

;; perspective-3d

(defn perspective-3d-render [canvas
                             {:keys [gl program vao matrix-location cnt]}
                             {:keys [tx ty]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
    (->> (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                    :aspect (/ gl.canvas.clientWidth
                                               gl.canvas.clientHeight)
                                    :near 1
                                    :far 2000})
         (u/multiply-matrices 4 (u/translation-matrix-3d tx ty -150))
         (u/multiply-matrices 4 (u/x-rotation-matrix-3d (u/deg->rad 180)))
         (u/multiply-matrices 4 (u/y-rotation-matrix-3d 0))
         (u/multiply-matrices 4 (u/z-rotation-matrix-3d 0))))
  (.drawArrays gl gl.TRIANGLES 0 cnt))

(defn perspective-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_color" (js/Uint8Array. (clj->js data/f-3d-colors))
                     {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
                   (u/create-buffer gl program "a_position"
                     (js/Float32Array. data/f-3d) {:size 3}))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt @*buffers}
        *state (atom {:tx 0 :ty 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
              y (- (.-height bounds)
                   (- (.-clientY event) (.-top bounds)))]
          (perspective-3d-render canvas props (swap! *state assoc :tx x :ty y)))))
    (perspective-3d-render canvas props @*state)))

(defexample play-cljc.examples-3d/perspective-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-3d-init)))

;; perspective-camera-3d

(defn perspective-camera-3d-render [canvas
                                    {:keys [gl program vao matrix-location cnt]}
                                    {:keys [r]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
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
        (.uniformMatrix4fv gl matrix-location false matrix)
        (.drawArrays gl gl.TRIANGLES 0 cnt)))))

(defn perspective-camera-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_color" (js/Uint8Array. (clj->js data/f-3d-colors))
                     {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
                   (let [positions (js/Float32Array. data/f-3d)
                         matrix (u/multiply-matrices 4
                                  (u/translation-matrix-3d -50 -75 -15)
                                  (u/x-rotation-matrix-3d js/Math.PI))]
                     (doseq [i (range 0 (.-length positions) 3)]
                       (let [v (u/transform-vector matrix
                                 (array
                                   (aget positions (+ i 0))
                                   (aget positions (+ i 1))
                                   (aget positions (+ i 2))
                                   1))]
                         (aset positions (+ i 0) (aget v 0))
                         (aset positions (+ i 1) (aget v 1))
                         (aset positions (+ i 2) (aget v 2))))
                     (u/create-buffer gl program "a_position" positions {:size 3})))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt @*buffers}
        *state (atom {:r 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              r (/ (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
                   (.-width bounds))]
          (perspective-camera-3d-render canvas props
            (swap! *state assoc :r (-> r (* 360) u/deg->rad))))))
    (perspective-camera-3d-render canvas props @*state)))

(defexample play-cljc.examples-3d/perspective-camera-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-camera-3d-init)))

;; perspective-camera-target-3d

(defn perspective-camera-target-3d-render [canvas
                                           {:keys [gl program vao matrix-location cnt]}
                                           {:keys [r]}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
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
        camera-pos (array
                     (aget camera-matrix 12)
                     (aget camera-matrix 13)
                     (aget camera-matrix 14))
        f-pos (array radius 0 0)
        up (array 0 1 0)
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
        (.uniformMatrix4fv gl matrix-location false matrix)
        (.drawArrays gl gl.TRIANGLES 0 cnt)))))

(defn perspective-camera-target-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_color" (js/Uint8Array. (clj->js data/f-3d-colors))
                     {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
                   (let [positions (js/Float32Array. data/f-3d)
                         matrix (u/multiply-matrices 4
                                  (u/translation-matrix-3d -50 -75 -15)
                                  (u/x-rotation-matrix-3d js/Math.PI))]
                     (doseq [i (range 0 (.-length positions) 3)]
                       (let [v (u/transform-vector matrix
                                 (array
                                   (aget positions (+ i 0))
                                   (aget positions (+ i 1))
                                   (aget positions (+ i 2))
                                   1))]
                         (aset positions (+ i 0) (aget v 0))
                         (aset positions (+ i 1) (aget v 1))
                         (aset positions (+ i 2) (aget v 2))))
                     (u/create-buffer gl program "a_position" positions {:size 3})))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt @*buffers}
        *state (atom {:r 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              r (/ (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
                   (.-width bounds))]
          (perspective-camera-target-3d-render canvas props
            (swap! *state assoc :r (-> r (* 360) u/deg->rad))))))
    (perspective-camera-target-3d-render canvas props @*state)))

(defexample play-cljc.examples-3d/perspective-camera-target-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-camera-target-3d-init)))

;; perspective-animation-3d

(defn perspective-animation-3d-render [canvas
                                       {:keys [gl program vao matrix-location cnt]
                                        :as props}
                                       {:keys [rx ry rz then now] :as state}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
    (->> (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                    :aspect (/ gl.canvas.clientWidth
                                               gl.canvas.clientHeight)
                                    :near 1
                                    :far 2000})
         (u/multiply-matrices 4 (u/translation-matrix-3d 0 0 -360))
         (u/multiply-matrices 4 (u/x-rotation-matrix-3d rx))
         (u/multiply-matrices 4 (u/y-rotation-matrix-3d ry))
         (u/multiply-matrices 4 (u/z-rotation-matrix-3d rz))))
  (.drawArrays gl gl.TRIANGLES 0 cnt)
  (js/requestAnimationFrame #(perspective-animation-3d-render canvas props
                               (-> state
                                   (update :ry + (* 1.2 (- now then)))
                                   (assoc :then now :now (* % 0.001))))))

(defn perspective-animation-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_color" (js/Uint8Array. (clj->js data/f-3d-colors))
                     {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
                   (u/create-buffer gl program "a_position"
                     (js/Float32Array. data/f-3d) {:size 3}))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt @*buffers}
        state {:rx (u/deg->rad 190)
               :ry (u/deg->rad 40)
               :rz (u/deg->rad 320)
               :then 0
               :now 0}]
    (perspective-animation-3d-render canvas props state)))

(defexample play-cljc.examples-3d/perspective-animation-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-animation-3d-init)))

;; perspective-texture-3d

(defn perspective-texture-3d-render [canvas
                                     {:keys [gl program vao matrix-location cnt]
                                      :as props}
                                     {:keys [rx ry then now] :as state}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (let [projection-matrix (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                                     :aspect (/ gl.canvas.clientWidth
                                                                gl.canvas.clientHeight)
                                                     :near 1
                                                     :far 2000})
        camera-pos (array 0 0 200)
        target (array 0 0 0)
        up (array 0 1 0)
        camera-matrix (u/look-at camera-pos target up)
        view-matrix (u/inverse-matrix 4 camera-matrix)
        view-projection-matrix (u/multiply-matrices 4 view-matrix projection-matrix)]
    (.uniformMatrix4fv gl matrix-location false
      (->> view-projection-matrix
           (u/multiply-matrices 4 (u/x-rotation-matrix-3d rx))
           (u/multiply-matrices 4 (u/y-rotation-matrix-3d ry))))
    (.drawArrays gl gl.TRIANGLES 0 cnt)
    (js/requestAnimationFrame #(perspective-texture-3d-render canvas props
                                 (-> state
                                     (update :rx + (* 1.2 (- now then)))
                                     (update :ry + (* 0.7 (- now then)))
                                     (assoc :then now :now (* % 0.001)))))))

(defn perspective-texture-3d-init [canvas image]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/texture-vertex-shader-source
                  data/texture-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_texcoord"
                     (js/Float32Array. data/f-texcoords) {:normalize true})
                   (let [positions (js/Float32Array. data/f-3d)
                         matrix (u/multiply-matrices 4
                                  (u/translation-matrix-3d -50 -75 -15)
                                  (u/x-rotation-matrix-3d js/Math.PI))]
                     (doseq [i (range 0 (.-length positions) 3)]
                       (let [v (u/transform-vector matrix
                                 (array
                                   (aget positions (+ i 0))
                                   (aget positions (+ i 1))
                                   (aget positions (+ i 2))
                                   1))]
                         (aset positions (+ i 0) (aget v 0))
                         (aset positions (+ i 1) (aget v 1))
                         (aset positions (+ i 2) (aget v 2))))
                     (u/create-buffer gl program "a_position" positions {:size 3})))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt @*buffers}
        state {:rx (u/deg->rad 190)
               :ry (u/deg->rad 40)
               :then 0
               :now 0}]
      (let [texture (.createTexture gl)]
        (.activeTexture gl (+ gl.TEXTURE0 0))
        (.bindTexture gl gl.TEXTURE_2D texture)
        (.texImage2D gl gl.TEXTURE_2D 0 gl.RGBA gl.RGBA gl.UNSIGNED_BYTE image)
        (.generateMipmap gl gl.TEXTURE_2D))
      (perspective-texture-3d-render canvas props state)))

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

(defn perspective-texture-data-3d-render [canvas
                                          {:keys [gl program vao matrix-location cnt]
                                           :as props}
                                          {:keys [rx ry then now] :as state}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (let [projection-matrix (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                                     :aspect (/ gl.canvas.clientWidth
                                                                gl.canvas.clientHeight)
                                                     :near 1
                                                     :far 2000})
        camera-pos (array 0 0 2)
        target (array 0 0 0)
        up (array 0 1 0)
        camera-matrix (u/look-at camera-pos target up)
        view-matrix (u/inverse-matrix 4 camera-matrix)
        view-projection-matrix (u/multiply-matrices 4 view-matrix projection-matrix)]
    (.uniformMatrix4fv gl matrix-location false
      (->> view-projection-matrix
           (u/multiply-matrices 4 (u/x-rotation-matrix-3d rx))
           (u/multiply-matrices 4 (u/y-rotation-matrix-3d ry))))
    (.drawArrays gl gl.TRIANGLES 0 cnt)
    (js/requestAnimationFrame #(perspective-texture-data-3d-render canvas props
                                 (-> state
                                     (update :rx + (* 1.2 (- now then)))
                                     (update :ry + (* 0.7 (- now then)))
                                     (assoc :then now :now (* % 0.001)))))))

(defn perspective-texture-data-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/texture-vertex-shader-source
                  data/texture-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_texcoord"
                     (js/Float32Array. data/cube-texcoords) {:normalize true})
                   (let [matrix (u/multiply-matrices 4
                                  (u/translation-matrix-3d -50 -75 -15)
                                  (u/x-rotation-matrix-3d js/Math.PI))
                         positions (js/Float32Array. data/cube)]
                     (u/create-buffer gl program "a_position" positions {:size 3})))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt @*buffers}
        state {:rx (u/deg->rad 190)
               :ry (u/deg->rad 40)
               :then 0
               :now 0}]
      (let [texture (.createTexture gl)
            level 0, internal-fmt gl.R8, width 3, height 2, border 0
            fmt gl.RED, type gl.UNSIGNED_BYTE
            data (js/Uint8Array. (array 128 64 128 0 192 0))]
        (.activeTexture gl (+ gl.TEXTURE0 0))
        (.bindTexture gl gl.TEXTURE_2D texture)
        (.pixelStorei gl gl.UNPACK_ALIGNMENT 1)
        (.texImage2D gl gl.TEXTURE_2D level internal-fmt width height border fmt type data)
        (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_MIN_FILTER gl.NEAREST)
        (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_MAG_FILTER gl.NEAREST)
        (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_WRAP_S gl.CLAMP_TO_EDGE)
        (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_WRAP_T gl.CLAMP_TO_EDGE))
      (perspective-texture-data-3d-render canvas props state)))

(defexample play-cljc.examples-3d/perspective-texture-data-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-texture-data-3d-init)))

;; perspective-texture-meta-3d

(defn draw-cube [{:keys [gl program vao matrix-location cnt]}
                 {:keys [rx ry]}
                 aspect]
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (let [projection-matrix (u/perspective-matrix-3d {:field-of-view (u/deg->rad 60)
                                                     :aspect aspect
                                                     :near 1
                                                     :far 2000})
        camera-pos (array 0 0 2)
        target (array 0 0 0)
        up (array 0 1 0)
        camera-matrix (u/look-at camera-pos target up)
        view-matrix (u/inverse-matrix 4 camera-matrix)
        view-projection-matrix (u/multiply-matrices 4 view-matrix projection-matrix)]
    (.uniformMatrix4fv gl matrix-location false
      (->> view-projection-matrix
           (u/multiply-matrices 4 (u/x-rotation-matrix-3d rx))
           (u/multiply-matrices 4 (u/y-rotation-matrix-3d ry))))
    (.drawArrays gl gl.TRIANGLES 0 cnt)))

(defn perspective-texture-meta-3d-render [canvas
                                          {:keys [gl program vao matrix-location cnt
                                                  textures]
                                           :as props}
                                          {:keys [then now] :as state}]
  (cu/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (doseq [{:keys [fb texture width height]
           [r g b a] :color}
          textures]
    (.bindFramebuffer gl gl.FRAMEBUFFER fb)
    (.bindTexture gl gl.TEXTURE_2D texture)
    (.viewport gl 0 0 width height)
    (.clearColor gl r g b a)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (draw-cube props state (/ width height)))
  (js/requestAnimationFrame #(perspective-texture-meta-3d-render canvas props
                               (-> state
                                   (update :rx + (* 1.2 (- now then)))
                                   (update :ry + (* 0.7 (- now then)))
                                   (assoc :then now :now (* % 0.001))))))

(defn perspective-texture-meta-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/texture-vertex-shader-source
                  data/texture-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_texcoord"
                     (js/Float32Array. data/cube-texcoords) {:normalize true})
                   (let [matrix (u/multiply-matrices 4
                                  (u/translation-matrix-3d -50 -75 -15)
                                  (u/x-rotation-matrix-3d js/Math.PI))
                         positions (js/Float32Array. data/cube)]
                     (u/create-buffer gl program "a_position" positions {:size 3})))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt @*buffers}
        state {:rx (u/deg->rad 190)
               :ry (u/deg->rad 40)
               :then 0
               :now 0}]
    (let [texture (.createTexture gl)
          level 0, internal-fmt gl.R8, width 3, height 2, border 0
          fmt gl.RED, type gl.UNSIGNED_BYTE
          data (js/Uint8Array. (array 128 64 128 0 192 0))]
      (.activeTexture gl (+ gl.TEXTURE0 0))
      (.bindTexture gl gl.TEXTURE_2D texture)
      (.pixelStorei gl gl.UNPACK_ALIGNMENT 1)
      (.texImage2D gl gl.TEXTURE_2D level internal-fmt width height border fmt type data)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_MIN_FILTER gl.NEAREST)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_MAG_FILTER gl.NEAREST)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_WRAP_S gl.CLAMP_TO_EDGE)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_WRAP_T gl.CLAMP_TO_EDGE)
      (let [target-texture (.createTexture gl)
            level 0, internal-fmt gl.RGBA, target-width 256, target-height 256, border 0
            fmt gl.RGBA, type gl.UNSIGNED_BYTE
            data nil]
        (.bindTexture gl gl.TEXTURE_2D target-texture)
        (.texImage2D gl gl.TEXTURE_2D level internal-fmt target-width target-height border fmt type data)
        (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_MIN_FILTER gl.LINEAR)
        (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_WRAP_S gl.CLAMP_TO_EDGE)
        (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_WRAP_T gl.CLAMP_TO_EDGE)
        (let [fb (.createFramebuffer gl)
              attachment-point gl.COLOR_ATTACHMENT0]
          (.bindFramebuffer gl gl.FRAMEBUFFER fb)
          (.framebufferTexture2D gl gl.FRAMEBUFFER attachment-point
            gl.TEXTURE_2D target-texture level)
          (let [props (assoc props :textures [{:fb fb
                                               :texture texture
                                               :width target-width
                                               :height target-height
                                               :color [0 0 1 1]}
                                              {:fb nil
                                               :texture target-texture
                                               :width gl.canvas.clientWidth
                                               :height gl.canvas.clientHeight
                                               :color [1 1 1 1]}])]
            (perspective-texture-meta-3d-render canvas props state)))))))

(defexample play-cljc.examples-3d/perspective-texture-meta-3d
  {:with-card card}
  (->> (play-cljc.client-utils/create-canvas card)
       (play-cljc.examples-3d/perspective-texture-meta-3d-init)))

