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
    (gl game deleteShader vertex-shader)
    (gl game deleteShader fragment-shader)
    (if #?(:clj (= (gl game TRUE) (gl game getProgrami program (gl game LINK_STATUS)))
           :cljs (gl game getProgramParameter program (gl game LINK_STATUS)))
      program
      (throw (ex-info (gl game getProgramInfoLog program) {})))))

(defn create-buffer [game]
  (gl game #?(:clj genBuffers :cljs createBuffer)))

(def ^:const float-size 4)

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

(defn get-uniform-type [{:keys [vertex fragment]} uni-name]
  (or (get-in vertex [:uniforms uni-name])
      (get-in fragment [:uniforms uni-name])
      (throw (ex-info "You must define the uniform in your vertex or fragment shader's :uniforms"
                      {:uniform-name uni-name}))))

(defn get-attribute-type [{:keys [vertex]} attr-name]
  (or (get-in vertex [:inputs attr-name])
      ;; for backwards compatibility
      (get-in vertex [:attributes attr-name])
      (throw (ex-info "You must define the attribute in your vertex shader's :inputs"
                      {:attribute-name attr-name}))))

(defn get-attribute-names [vertex]
  (or (some-> vertex :inputs keys)
      ;; for backwards compatibility
      (some-> vertex :attributes keys)))

(def default-opts {:iter 1 :normalize false :stride 0 :offset 0 :divisor 0})
(def type->attribute-opts
  '{float {:size 1}
    vec2  {:size 2}
    vec3  {:size 3}
    vec4  {:size 4}
    mat2  {:size 4}
    mat3  {:size 3
           :iter 3}
    mat4  {:size 4
           :iter 4}})

(defn merge-attribute-opts [entity attr-name opts]
  (let [type-name (get-attribute-type entity attr-name)]
    (merge default-opts
           (type->attribute-opts type-name)
           opts)))

(defn assoc-instance-attr [index entity instanced-entity attr-name uni-name]
  (when (:program entity)
    (throw (ex-info "Only uncompiled entities can be assoc'ed to an instanced entity" {})))
  (let [new-data (get-in entity [:uniforms uni-name])
        data-len (count new-data)
        offset (* index data-len)]
    (update-in instanced-entity [:attributes attr-name]
               (fn [attr]
                 (if attr
                   (update attr :data
                           (fn [old-data]
                             (persistent!
                               (reduce-kv
                                 (fn [data i n]
                                   (assoc! data (+ offset i) n))
                                 (transient old-data)
                                 new-data))))
                   {:data (vec new-data)
                    :divisor 1})))))

(defn dissoc-instance-attr [index instanced-entity attr-name]
  (update-in instanced-entity [:attributes attr-name]
             (fn [attr]
               (let [{:keys [size iter]} (merge-attribute-opts instanced-entity attr-name attr)
                     data-len (* size iter)
                     offset (* index data-len)]
                 (update attr :data
                         (fn [data]
                           (let [v1 (subvec data 0 offset)
                                 v2 (subvec data (+ offset data-len))]
                             (into (into [] v1) v2))))))))

