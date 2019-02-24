(ns play-cljc.examples-2d
  (:require [play-cljc.utils :as u]
            [goog.events :as events]
            [play-cljc.data :as data])
  (:require-macros [dynadoc.example :refer [defexample]]))

;; rand-rects

(defn rand-rects-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_position" (js/Float32Array. data/rect)))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        color-location (.getUniformLocation gl program "u_color")
        cnt @*buffers]
    (u/resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (dotimes [_ 50]
      (.uniform4f gl color-location (rand) (rand) (rand) 1)
      (.uniformMatrix3fv gl matrix-location false
        (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
             (u/multiply-matrices 3 (u/translation-matrix (rand-int 300) (rand-int 300)))
             (u/multiply-matrices 3 (u/scaling-matrix (rand-int 300) (rand-int 300)))))
      (.drawArrays gl gl.TRIANGLES 0 cnt))))

(defexample play-cljc.examples-2d/rand-rects
  {:with-card card}
  (->> (play-cljc.dev/create-canvas card)
       (play-cljc.examples-2d/rand-rects-init)))

;; image

(defn image-init [canvas image]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/image-vertex-shader-source
                  data/image-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_position" (js/Float32Array. data/rect)))
        vao (u/create-vao gl *buffers)
        matrix-location (.getUniformLocation gl program "u_matrix")
        image-location (.getUniformLocation gl program "u_image")
        texture-unit 0
        cnt @*buffers]
    (let [texture (.createTexture gl)]
      (.activeTexture gl (+ gl.TEXTURE0 texture-unit))
      (.bindTexture gl gl.TEXTURE_2D texture)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_WRAP_S gl.CLAMP_TO_EDGE)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_WRAP_T gl.CLAMP_TO_EDGE)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_MIN_FILTER gl.NEAREST)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_MAG_FILTER gl.NEAREST))
    (let [mip-level 0, internal-fmt gl.RGBA, src-fmt gl.RGBA, src-type gl.UNSIGNED_BYTE]
      (.texImage2D gl gl.TEXTURE_2D mip-level internal-fmt src-fmt src-type image))
    (u/resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniform1i gl image-location texture-unit)
    (.uniformMatrix3fv gl matrix-location false
      (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
           (u/multiply-matrices 3 (u/translation-matrix 0 0))
           (u/multiply-matrices 3 (u/scaling-matrix image.width image.height))))
    (.drawArrays gl gl.TRIANGLES 0 cnt)))

(defn image-load [canvas]
  (let [image (js/Image.)]
    (doto image
      (-> .-src (set! "leaves.jpg"))
      (-> .-onload (set! (fn []
                           (image-init canvas image)))))))

(defexample play-cljc.examples-2d/image
  {:with-card card}
  (->> (play-cljc.dev/create-canvas card)
       (play-cljc.examples-2d/image-load)))

;; translation

(defn translation-render [canvas
                          {:keys [gl program vao matrix-location color-location cnt]}
                          {:keys [x y]}]
  (u/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniform4f gl color-location 1 0 0.5 1)
  (.uniformMatrix3fv gl matrix-location false
    (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
         (u/multiply-matrices 3 (u/translation-matrix x y))))
  (.drawArrays gl gl.TRIANGLES 0 cnt))

(defn translation-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_position" (js/Float32Array. data/f-2d)))
        vao (u/create-vao gl *buffers)
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :color-location color-location
               :matrix-location matrix-location
               :cnt @*buffers}
        *state (atom {:x 0 :y 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds))
              y (- (.-clientY event) (.-top bounds))]
          (translation-render canvas props (swap! *state assoc :x x :y y)))))
    (translation-render canvas props @*state)))

(defexample play-cljc.examples-2d/translation
  {:with-card card}
  (->> (play-cljc.dev/create-canvas card)
       (play-cljc.examples-2d/translation-init)))

;; rotation

(defn rotation-render [canvas
                       {:keys [gl program vao matrix-location color-location cnt]}
                       {:keys [tx ty r]}]
  (u/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniform4f gl color-location 1 0 0.5 1)
  (.uniformMatrix3fv gl matrix-location false
    (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
         (u/multiply-matrices 3 (u/translation-matrix tx ty))
         (u/multiply-matrices 3 (u/rotation-matrix r))
         ;; make it rotate around its center
         (u/multiply-matrices 3 (u/translation-matrix -50 -75))))
  (.drawArrays gl gl.TRIANGLES 0 cnt))

(defn rotation-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_position" (js/Float32Array. data/f-2d)))
        vao (u/create-vao gl *buffers)
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :color-location color-location
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
          (rotation-render canvas props (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-render canvas props @*state)))

(defexample play-cljc.examples-2d/rotation
  {:with-card card}
  (->> (play-cljc.dev/create-canvas card)
       (play-cljc.examples-2d/rotation-init)))

;; scale

(defn scale-render [canvas
                    {:keys [gl program vao matrix-location color-location cnt]}
                    {:keys [tx ty sx sy]}]
  (u/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniform4f gl color-location 1 0 0.5 1)
  (.uniformMatrix3fv gl matrix-location false
    (->> (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
         (u/multiply-matrices 3 (u/translation-matrix tx ty))
         (u/multiply-matrices 3 (u/rotation-matrix 0))
         (u/multiply-matrices 3 (u/scaling-matrix sx sy))))
  (.drawArrays gl gl.TRIANGLES 0 cnt))

(defn scale-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_position" (js/Float32Array. data/f-2d)))
        vao (u/create-vao gl *buffers)
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :color-location color-location
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
          (scale-render canvas props (swap! *state assoc :sx sx :sy sy)))))
    (scale-render canvas props @*state)))

(defexample play-cljc.examples-2d/scale
  {:with-card card}
  (->> (play-cljc.dev/create-canvas card)
       (play-cljc.examples-2d/scale-init)))

;; rotation-multi

(defn rotation-multi-render [canvas
                             {:keys [gl program vao matrix-location color-location cnt]}
                             {:keys [tx ty r]}]
  (u/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniform4f gl color-location 1 0 0.5 1)
  (loop [i 0
         matrix (u/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)]
    (when (< i 5)
      (let [matrix (->> matrix
                        (u/multiply-matrices 3 (u/translation-matrix tx ty))
                        (u/multiply-matrices 3 (u/rotation-matrix r)))]
        (.uniformMatrix3fv gl matrix-location false matrix)
        (.drawArrays gl gl.TRIANGLES 0 cnt)
        (recur (inc i) matrix)))))

(defn rotation-multi-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (u/create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        *buffers (delay
                   (u/create-buffer gl program "a_position" (js/Float32Array. data/f-2d)))
        vao (u/create-vao gl *buffers)
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :color-location color-location
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
          (rotation-multi-render canvas props (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-multi-render canvas props @*state)))

(defexample play-cljc.examples-2d/rotation-multi
  {:with-card card}
  (->> (play-cljc.dev/create-canvas card)
       (play-cljc.examples-2d/rotation-multi-init)))

