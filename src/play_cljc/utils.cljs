(ns play-cljc.utils)

(defn create-shader [gl type source]
  (let [shader (.createShader gl type)]
    (.shaderSource gl shader source)
    (.compileShader gl shader)
    (if (.getShaderParameter gl shader gl.COMPILE_STATUS)
      shader
      (do
        (js/console.log (.getShaderInfoLog gl shader))
        (.deleteShader gl shader)))))

(defn create-program [gl v-source f-source]
  (let [vertex-shader (create-shader gl gl.VERTEX_SHADER v-source)
        fragment-shader (create-shader gl gl.FRAGMENT_SHADER f-source)
        program (.createProgram gl)]
    (.attachShader gl program vertex-shader)
    (.attachShader gl program fragment-shader)
    (.linkProgram gl program)
    (if (.getProgramParameter gl program gl.LINK_STATUS)
      program
      (do
        (js/console.log (.getProgramInfoLog gl program))
        (.deleteProgram gl program)))))

(defn create-buffer
  ([gl program attrib-name src-data]
   (create-buffer gl program attrib-name src-data {}))
  ([gl program attrib-name src-data
    {:keys [size type normalize stride offset]
     :or {size 2
          type gl.FLOAT
          normalize false
          stride 0
          offset 0}}]
   (let [attrib-location (.getAttribLocation gl program attrib-name)
         buffer (.createBuffer gl)]
     (.bindBuffer gl gl.ARRAY_BUFFER buffer)
     (.enableVertexAttribArray gl attrib-location)
     (.vertexAttribPointer gl attrib-location size type normalize stride offset)
     (.bindBuffer gl gl.ARRAY_BUFFER buffer)
     (.bufferData gl gl.ARRAY_BUFFER src-data gl.STATIC_DRAW)
     (/ (.-length src-data) size))))

(defn create-index-buffer [gl indices]
  (let [index-buffer (.createBuffer gl)]
    (.bindBuffer gl gl.ELEMENT_ARRAY_BUFFER index-buffer)
    (.bufferData gl gl.ELEMENT_ARRAY_BUFFER indices gl.STATIC_DRAW)
    (.-length indices)))

(defn create-vao [gl *create-buffers]
   (let [vao (.createVertexArray gl)]
     (.bindVertexArray gl vao)
     @*create-buffers
     (.bindVertexArray gl nil)
     vao))

(defn multiply-matrices [size m1 m2]
  (let [m1 (mapv vec (partition size m1))
        m2 (mapv vec (partition size m2))
        result (for [i (range size)
                     j (range size)]
                 (reduce
                   (fn [sum k]
                     (+ sum (* (get-in m1 [i k])
                               (get-in m2 [k j]))))
                   0
                   (range size)))]
    (clj->js result)))

(defn inverse-matrix [size m]
  (let [mc (mapv vec (partition size m))
        mi (mapv vec (for [i (range size)]
                       (for [j (range size)]
                         (if (= i j) 1 0))))
        mc (clj->js mc)
        mi (clj->js mi)]
    (dotimes [i size]
      (when (= 0 (aget mc i i))
        (loop [r (range (+ i 1) size)]
          (when-let [ii (first r)]
            (if (not= 0 (aget mc ii i))
              (dotimes [j size]
                (let [e (aget mc i j)]
                  (aset mc i j (aget mc ii j))
                  (aset mc ii j e))
                (let [e (aget mi i j)]
                  (aset mi i j (aget mi ii j))
                  (aset mi ii j e)))
              (recur (rest r))))))
      (let [e (aget mc i i)]
        (when (= 0 e)
          (throw (js/Error. "Not invertable")))
        (dotimes [j size]
          (aset mc i j (/ (aget mc i j) e))
          (aset mi i j (/ (aget mi i j) e))))
      (dotimes [ii size]
        (when (not= i ii)
          (let [e (aget mc ii i)]
            (dotimes [j size]
              (aset mc ii j
                (- (aget mc ii j)
                   (* e (aget mc i j))))
              (aset mi ii j
                (- (aget mi ii j)
                   (* e (aget mi i j)))))))))
    (->> mi seq (map seq) flatten clj->js)))

(defn deg->rad [d]
  (-> d (* js/Math.PI) (/ 180)))

(defn transform-vector [m v]
  (let [dst (array)]
    (dotimes [i 4]
      (aset dst i 0.0)
      (dotimes [j 4]
        (aset dst i
          (+ (aget dst i)
             (* (aget v j)
                (aget m (-> j (* 4) (+ i))))))))
    dst))

;; two-d

(defn translation-matrix [tx ty]
  (array
    1 0 0
    0 1 0
    tx ty 1))

(defn rotation-matrix [angle-in-radians]
  (let [c (js/Math.cos angle-in-radians)
        s (js/Math.sin angle-in-radians)]
    (array
      c (- s) 0
      s c 0
      0 0 1)))

(defn scaling-matrix [sx sy]
  (array
    sx 0 0
    0 sy 0
    0 0 1))

(defn projection-matrix [width height]
  (array
    (/ 2 width) 0 0
    0 (/ -2 height) 0
    -1 1 1))

;; three-d

(defn translation-matrix-3d [tx ty tz]
  (array
    1,  0,  0,  0,
    0,  1,  0,  0,
    0,  0,  1,  0,
    tx, ty, tz, 1,))

(defn x-rotation-matrix-3d [angle-in-radians]
  (let [c (js/Math.cos angle-in-radians)
        s (js/Math.sin angle-in-radians)]
    (array
      1, 0, 0, 0,
      0, c, s, 0,
      0, (- s), c, 0,
      0, 0, 0, 1)))

(defn y-rotation-matrix-3d [angle-in-radians]
  (let [c (js/Math.cos angle-in-radians)
        s (js/Math.sin angle-in-radians)]
    (array
      c, 0, (- s), 0,
      0, 1, 0, 0,
      s, 0, c, 0,
      0, 0, 0, 1,)))

(defn z-rotation-matrix-3d [angle-in-radians]
  (let [c (js/Math.cos angle-in-radians)
        s (js/Math.sin angle-in-radians)]
    (array
      c, s, 0, 0,
      (- s), c, 0, 0,
      0, 0, 1, 0,
      0, 0, 0, 1,)))

(defn scaling-matrix-3d [sx sy sz]
  (array
    sx, 0,  0,  0,
    0, sy,  0,  0,
    0,  0, sz,  0,
    0,  0,  0,  1,))

(defn ortho-matrix-3d [{:keys [left right bottom top near far]}]
  (let [width (- right left)
        height (- top bottom)
        depth (- near far)]
    (array
      (/ 2 width) 0 0 0
      0 (/ 2 height) 0 0
      0 0 (/ 2 depth) 0
      
      (/ (+ left right)
         (- left right))
      (/ (+ bottom top)
         (- bottom top))
      (/ (+ near far)
         (- near far))
      1)))

(defn perspective-matrix-3d [{:keys [field-of-view aspect near far]}]
  (let [f (js/Math.tan (- (* js/Math.PI 0.5)
                          (* field-of-view 0.5)))
        range-inv (/ 1 (- near far))]
    (array
      (/ f aspect) 0 0 0
      0 f 0 0
      0 0 (* (+ near far) range-inv) -1
      0 0 (* near far range-inv 2) 0)))

(defn identity-matrix-3d []
  (array
    1 0 0 0
    0 1 0 0
    0 0 1 0
    0 0 0 1))

(defn transpose-matrix-3d [m]
  (array
    (aget m 0) (aget m 4) (aget m 8) (aget m 12)
    (aget m 1) (aget m 5) (aget m 9) (aget m 13)
    (aget m 2) (aget m 6) (aget m 10) (aget m 14)
    (aget m 3) (aget m 7) (aget m 11) (aget m 15)))

(defn cross [a b]
  (array
    (- (* (aget a 1) (aget b 2))
       (* (aget a 2) (aget b 1)))
    (- (* (aget a 2) (aget b 0))
       (* (aget a 0) (aget b 2)))
    (- (* (aget a 0) (aget b 1))
       (* (aget a 1) (aget b 0)))))

(defn subtract-vectors [a b]
  (array
    (- (aget a 0) (aget b 0))
    (- (aget a 1) (aget b 1))
    (- (aget a 2) (aget b 2))))

(defn normalize [v]
  (let [length (js/Math.sqrt
                 (+ (* (aget v 0) (aget v 0))
                    (* (aget v 1) (aget v 1))
                    (* (aget v 2) (aget v 2))))]
    (if (> length 0.00001)
      (array
        (/ (aget v 0) length)
        (/ (aget v 1) length)
        (/ (aget v 2) length))
      (array 0 0 0))))

(defn look-at [camera-pos target up]
  (let [z-axis (normalize (subtract-vectors camera-pos target))
        x-axis (normalize (cross up z-axis))
        y-axis (normalize (cross z-axis x-axis))]
    (array
      (aget x-axis 0) (aget x-axis 1) (aget x-axis 2) 0
      (aget y-axis 0) (aget y-axis 1) (aget y-axis 2) 0
      (aget z-axis 0) (aget z-axis 1) (aget z-axis 2) 0
      (aget camera-pos 0) (aget camera-pos 1) (aget camera-pos 2) 1)))

