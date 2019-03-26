(ns play-cljc.gl.utils
  (:require #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])))

(defn- create-shader [game type source]
  (let [shader (gl game createShader type)]
    (gl game shaderSource shader source)
    (gl game compileShader shader)
    (if #?(:clj (= (gl game TRUE) (gl game getShaderi shader (gl game COMPILE_STATUS)))
           :cljs (gl game getShaderParameter shader (gl game COMPILE_STATUS)))
      shader
      (throw (ex-info (gl game getShaderInfoLog shader) {})))))

(defn create-program [game v-source f-source]
  (let [vertex-shader (create-shader game (gl game VERTEX_SHADER) v-source)
        fragment-shader (create-shader game (gl game FRAGMENT_SHADER) f-source)
        program (gl game createProgram)]
    (gl game attachShader program vertex-shader)
    (gl game attachShader program fragment-shader)
    (gl game linkProgram program)
    (if #?(:clj (= (gl game TRUE) (gl game getProgrami program (gl game LINK_STATUS)))
           :cljs (gl game getProgramParameter program (gl game LINK_STATUS)))
      program
      (throw (ex-info (gl game getProgramInfoLog program) {})))))

(defn create-buffer
  ([game program attrib-name src-data]
   (create-buffer game program attrib-name src-data {}))
  ([game program attrib-name src-data
    {:keys [size type normalize stride offset]
     :or {normalize false stride 0 offset 0}}]
   (let [attrib-location (gl game getAttribLocation program attrib-name)
         buffer (gl game #?(:clj genBuffers :cljs createBuffer))]
     (gl game bindBuffer (gl game ARRAY_BUFFER) buffer)
     (gl game enableVertexAttribArray attrib-location)
     (gl game vertexAttribPointer attrib-location size type normalize stride offset)
     (gl game bufferData (gl game ARRAY_BUFFER) src-data (gl game STATIC_DRAW))
     (/ (#?(:clj count :cljs .-length) src-data) size))))

(defn create-index-buffer [game indices]
  (let [index-buffer (gl game #?(:clj genBuffers :cljs createBuffer))]
    (gl game bindBuffer (gl game ELEMENT_ARRAY_BUFFER) index-buffer)
    (gl game bufferData (gl game ELEMENT_ARRAY_BUFFER) indices (gl game STATIC_DRAW))
    (#?(:clj count :cljs .-length) indices)))

