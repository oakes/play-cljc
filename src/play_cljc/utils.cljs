(ns play-cljc.utils)

(defn- create-shader [gl type source]
  (let [shader (.createShader gl type)]
    (.shaderSource gl shader source)
    (.compileShader gl shader)
    (if (.getShaderParameter gl shader gl.COMPILE_STATUS)
      shader
      (throw (js/Error. (.getShaderInfoLog gl shader))))))

(defn create-program [{:keys [gl]} v-source f-source]
  (let [vertex-shader (create-shader gl gl.VERTEX_SHADER v-source)
        fragment-shader (create-shader gl gl.FRAGMENT_SHADER f-source)
        program (.createProgram gl)]
    (.attachShader gl program vertex-shader)
    (.attachShader gl program fragment-shader)
    (.linkProgram gl program)
    (if (.getProgramParameter gl program gl.LINK_STATUS)
      program
      (throw (js/Error. (.getProgramInfoLog gl program))))))

(defn create-buffer
  ([game program attrib-name src-data]
   (create-buffer game program attrib-name src-data {}))
  ([{:keys [gl]} program attrib-name src-data
    {:keys [size type normalize stride offset]}]
   (let [attrib-location (.getAttribLocation gl program attrib-name)
         buffer (.createBuffer gl)]
     (.bindBuffer gl gl.ARRAY_BUFFER buffer)
     (.enableVertexAttribArray gl attrib-location)
     (.vertexAttribPointer gl attrib-location size type normalize stride offset)
     (.bindBuffer gl gl.ARRAY_BUFFER buffer)
     (.bufferData gl gl.ARRAY_BUFFER src-data gl.STATIC_DRAW)
     (/ (.-length src-data) size))))

(defn create-index-buffer [{:keys [gl]} indices]
  (let [index-buffer (.createBuffer gl)]
    (.bindBuffer gl gl.ELEMENT_ARRAY_BUFFER index-buffer)
    (.bufferData gl gl.ELEMENT_ARRAY_BUFFER indices gl.STATIC_DRAW)
    (.-length indices)))

(defn get-width [{:keys [gl]}]
  gl.canvas.clientWidth)

(defn get-height [{:keys [gl]}]
  gl.canvas.clientHeight)

(defn get-enum [{:keys [gl]} k]
  (aget gl (name k)))

(defn enable [{:keys [gl] :as game} k]
  (.enable gl (get-enum game k)))

