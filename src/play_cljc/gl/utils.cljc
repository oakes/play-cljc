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

(defn create-buffer [game]
  (gl game #?(:clj genBuffers :cljs createBuffer)))

(def ^:const float-size 4)
(def ^:const default-opts {:iter 1 :normalize false :stride 0 :offset 0 :divisor 0})

(defn set-array-buffer [game program buffer attrib-name data
                        {:keys [size type iter normalize stride offset divisor] :as opts}]
  (let [attrib-location (gl game getAttribLocation program attrib-name)
        previous-buffer (gl game #?(:clj getInteger :cljs getParameter)
                          (gl game ARRAY_BUFFER_BINDING))
        total-size (* size iter)]
    (gl game bindBuffer (gl game ARRAY_BUFFER) buffer)
    (gl game bufferData (gl game ARRAY_BUFFER) data (gl game STATIC_DRAW))
    (dotimes [i iter]
      (let [loc (+ attrib-location i)]
        (gl game enableVertexAttribArray loc)
        (gl game vertexAttribPointer loc size type normalize (* total-size float-size) (* i size float-size))
        (gl game vertexAttribDivisor loc divisor)))
    (gl game bindBuffer (gl game ARRAY_BUFFER) previous-buffer)
    (/ (#?(:clj count :cljs .-length) data) total-size)))

(defn set-index-buffer [game index-buffer indices]
  (let [previous-index-buffer (gl game #?(:clj getInteger :cljs getParameter)
                                (gl game ELEMENT_ARRAY_BUFFER_BINDING))]
    (gl game bindBuffer (gl game ELEMENT_ARRAY_BUFFER) index-buffer)
    (gl game bufferData (gl game ELEMENT_ARRAY_BUFFER) indices (gl game STATIC_DRAW))
    (gl game bindBuffer (gl game ELEMENT_ARRAY_BUFFER) previous-index-buffer)
    (#?(:clj count :cljs .-length) indices)))

(defn assoc-instance-attr [index entity {:keys [instance-count] :as instanced-entity} attr-name uni-name]
  (let [new-data (get-in entity [:uniforms uni-name])
        data-len (count new-data)
        total-len (* data-len instance-count)
        offset (* index data-len)]
    (when (>= index instance-count)
      (throw (ex-info "Attempted to assoc at an index that is >= the instance-count"
                      {:index index
                       :instance-count instance-count})))
    (update-in instanced-entity [:attributes attr-name]
               (fn [attr]
                 (if attr
                   (update attr :data
                           (fn [old-data]
                             (let [old-data (cond-> old-data
                                                    (> (count old-data) total-len)
                                                    (subvec 0 total-len))]
                               (persistent!
                                 (reduce-kv
                                   (fn [data i n]
                                     (assoc! data (+ offset i) n))
                                   (transient old-data)
                                   new-data)))))
                   {:data (vec new-data)
                    :divisor 1})))))

