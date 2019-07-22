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

(def ^:const float-size 4)

(defn create-buffer
  ([game program attrib-name data]
   (create-buffer game program attrib-name data {}))
  ([game program attrib-name data
    {:keys [size type iter normalize stride offset divisor]
     :or {iter 1 normalize false stride 0 offset 0 divisor 0}}]
   (let [attrib-location (gl game getAttribLocation program attrib-name)
         previous-buffer (gl game #?(:clj getInteger :cljs getParameter)
                           (gl game ARRAY_BUFFER_BINDING))
         buffer (gl game #?(:clj genBuffers :cljs createBuffer))
         total-size (* size iter)]
     (gl game bindBuffer (gl game ARRAY_BUFFER) buffer)
     (gl game bufferData (gl game ARRAY_BUFFER) data (gl game STATIC_DRAW))
     (dotimes [i iter]
       (let [loc (+ attrib-location i)]
         (gl game enableVertexAttribArray loc)
         (gl game vertexAttribPointer loc size type normalize (* total-size float-size) (* i size float-size))
         (gl game vertexAttribDivisor loc divisor)))
     (gl game bindBuffer (gl game ARRAY_BUFFER) previous-buffer)
     {:buffer buffer
      :draw-count (/ (#?(:clj count :cljs .-length) data) total-size)})))

(defn create-index-buffer [game indices]
  (let [previous-index-buffer (gl game #?(:clj getInteger :cljs getParameter)
                                (gl game ELEMENT_ARRAY_BUFFER_BINDING))
        index-buffer (gl game #?(:clj genBuffers :cljs createBuffer))]
    (gl game bindBuffer (gl game ELEMENT_ARRAY_BUFFER) index-buffer)
    (gl game bufferData (gl game ELEMENT_ARRAY_BUFFER) indices (gl game STATIC_DRAW))
    (gl game bindBuffer (gl game ELEMENT_ARRAY_BUFFER) previous-index-buffer)
    {:buffer index-buffer
     :draw-count (#?(:clj count :cljs .-length) indices)}))

