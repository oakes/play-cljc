(ns play-cljc.core
  (:require [iglu.core :as ig]
            [iglu.parse :as parse]
            [play-cljc.utils :as u]))

(defn glsl-type->platform-type [glsl-type]
  (case glsl-type
    (vec2 vec3 vec4)    js/Float32Array
    (dvec2 dvec3 dvec4) js/Float64Array
    (ivec2 ivec3 ivec4) js/Int32Array
    (uvec2 uvec3 uvec4) js/Uint32Array))

(defn init-texture [gl m uni-loc {:keys [image params]}]
  (let [unit (count (:textures m))
        texture (.createTexture gl)]
    (.activeTexture gl (+ gl.TEXTURE0 unit))
    (.bindTexture gl gl.TEXTURE_2D texture)
    (doseq [[param-name param-val] params]
      (.texParameteri gl gl.TEXTURE_2D param-name param-val))
    (let [mip-level 0, internal-fmt gl.RGBA, src-fmt gl.RGBA, src-type gl.UNSIGNED_BYTE]
      (.texImage2D gl gl.TEXTURE_2D mip-level internal-fmt src-fmt src-type image))
    (update m :textures conj uni-loc)))

(defn call-uniform* [gl m glsl-type uni-loc data]
  (case glsl-type
    vec2 (.uniform2fv gl uni-loc data)
    vec3 (.uniform3fv gl uni-loc data)
    vec4 (.uniform4fv gl uni-loc data)
    mat2 (.uniformMatrix2fv gl uni-loc false data)
    mat3 (.uniformMatrix3fv gl uni-loc false data)
    mat4 (.uniformMatrix4fv gl uni-loc false data)
    sampler2D (init-texture gl m uni-loc data)))

(defn get-uniform-type [{:keys [vertex fragment]} uni-name]
  (or (get-in vertex [:uniforms uni-name])
      (get-in fragment [:uniforms uni-name])
      (parse/throw-error
        (str "You must define " uni-name
          " in your vertex or fragment shader"))))

(defn call-uniform [gl {:keys [uniform-locations] :as m} [uni-name uni-data]]
  (let [uni-type (get-uniform-type m uni-name)
        uni-loc (get uniform-locations uni-name)]
    (or (call-uniform* gl m uni-type uni-loc uni-data)
        m)))

(defn create-entity [gl {:keys [vertex fragment attributes uniforms] :as m}]
  (let [vertex-source (ig/iglu->glsl :vertex vertex)
        fragment-source (ig/iglu->glsl :fragment fragment)
        program (u/create-program gl vertex-source fragment-source)
        vao (.createVertexArray gl)
        _ (.bindVertexArray gl vao)
        counts (mapv (fn [[attr-name {:keys [data] :as opts}]]
                       (let [attr-type (or (get-in vertex [:attributes attr-name])
                                           (parse/throw-error
                                             (str "You must define " attr-name
                                               " in your vertex shader")))
                             attr-type (or (glsl-type->platform-type attr-type)
                                           (parse/throw-error
                                             (str "The type " attr-type
                                               " is invalid for attribute " attr-name)))]
                         (u/create-buffer gl program (name attr-name)
                           (new attr-type data)
                           opts)))
                 attributes)
        uniform-locations (reduce
                            (fn [m uniform]
                              (assoc m uniform
                                (.getUniformLocation gl program (name uniform))))
                            {}
                            (-> #{}
                                (into (-> vertex :uniforms keys))
                                (into (-> fragment :uniforms keys))))
        entity (assoc m
                 :vertex-source vertex-source
                 :fragment-source fragment-source
                 :program program
                 :vao vao
                 :uniform-locations uniform-locations
                 :textures []
                 :index-count (apply max counts))
        entity (reduce
                 (partial call-uniform gl)
                 entity
                 uniforms)]
    (.bindVertexArray gl nil)
    entity))

(defn render-entity [gl
                     {:keys [program vao index-count textures] :as entity}
                     {:keys [uniforms]}]
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (reduce
    (partial call-uniform gl)
    entity
    uniforms)
  (dotimes [i (range (count textures))]
    (.uniform1i gl (nth textures i) i))
  (.drawArrays gl gl.TRIANGLES 0 index-count)
  (.bindVertexArray gl nil))

