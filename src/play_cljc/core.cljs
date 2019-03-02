(ns play-cljc.core
  (:require [iglu.core :as ig]
            [iglu.parse :as parse]
            [play-cljc.utils :as u]))

(defn create-game [gl]
  {:tex-count (atom 0)
   :gl gl})

(defprotocol Renderable
  (render [this game]))

(defrecord Entity
  [vertex fragment
   vertex-source fragment-source
   program vao
   uniform-locations texture-locations
   index-count])

(defn glsl-type->platform-type [glsl-type]
  (case glsl-type
    (vec2 vec3 vec4)    js/Float32Array
    (dvec2 dvec3 dvec4) js/Float64Array
    (ivec2 ivec3 ivec4) js/Int32Array
    (uvec2 uvec3 uvec4) js/Uint32Array))

(defn init-texture [{:keys [gl tex-count]} m uni-loc uni-name {:keys [data params opts mipmap alignment]}]
  (let [unit (dec (swap! tex-count inc))
        texture (.createTexture gl)]
    (.activeTexture gl (+ gl.TEXTURE0 unit))
    (.bindTexture gl gl.TEXTURE_2D texture)
    (doseq [[param-name param-val] params]
      (.texParameteri gl gl.TEXTURE_2D param-name param-val))
    (when alignment
      (.pixelStorei gl gl.UNPACK_ALIGNMENT alignment))
    (let [{:keys [mip-level internal-fmt src-fmt src-type width height border]} opts]
      (if (and width height border)
        (.texImage2D gl gl.TEXTURE_2D mip-level internal-fmt width height border src-fmt src-type data)
        (.texImage2D gl gl.TEXTURE_2D mip-level internal-fmt src-fmt src-type data)))
    (when mipmap
      (.generateMipmap gl gl.TEXTURE_2D))
    (update m :textures assoc uni-name {:unit unit
                                        :texture texture
                                        :location uni-loc})))

(defn call-uniform* [{:keys [gl] :as game} m glsl-type uni-loc uni-name data]
  (case glsl-type
    vec2 (.uniform2fv gl uni-loc data)
    vec3 (.uniform3fv gl uni-loc data)
    vec4 (.uniform4fv gl uni-loc data)
    mat2 (.uniformMatrix2fv gl uni-loc false data)
    mat3 (.uniformMatrix3fv gl uni-loc false data)
    mat4 (.uniformMatrix4fv gl uni-loc false data)
    sampler2D (init-texture game m uni-loc uni-name data)))

(defn get-uniform-type [{:keys [vertex fragment]} uni-name]
  (or (get-in vertex [:uniforms uni-name])
      (get-in fragment [:uniforms uni-name])
      (parse/throw-error
        (str "You must define " uni-name
          " in your vertex or fragment shader"))))

(defn call-uniform [game {:keys [uniform-locations] :as m} [uni-name uni-data]]
  (let [uni-type (get-uniform-type m uni-name)
        uni-loc (get uniform-locations uni-name)]
    (or (call-uniform* game m uni-type uni-loc uni-name uni-data)
        m)))

(defn create-entity [{:keys [vertex fragment attributes uniforms] :as m} {:keys [gl] :as game}]
  (let [vertex-source (ig/iglu->glsl :vertex vertex)
        fragment-source (ig/iglu->glsl :fragment fragment)
        program (u/create-program gl vertex-source fragment-source)
        _ (.useProgram gl program)
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
                           (if (js/ArrayBuffer.isView data)
                             data
                             (new attr-type data))
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
        entity (map->Entity {:vertex vertex
                             :fragment fragment
                             :vertex-source vertex-source
                             :fragment-source fragment-source
                             :program program
                             :vao vao
                             :uniform-locations uniform-locations
                             :textures {}
                             :index-count (apply max counts)})
        entity (reduce
                 (partial call-uniform game)
                 entity
                 uniforms)]
    (.bindVertexArray gl nil)
    entity))

(extend-type Entity
  Renderable
  (render [{:keys [program vao index-count uniforms] :as entity} {:keys [gl] :as game}]
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (let [{:keys [textures]} (reduce
                               (partial call-uniform game)
                               entity
                               uniforms)]
      (doseq [{:keys [unit location]} (vals textures)]
        (.uniform1i gl location unit)))
    (.drawArrays gl gl.TRIANGLES 0 index-count)
    (.bindVertexArray gl nil)))

(defrecord Clear [color depth stencil])

(extend-type Clear
  Renderable
  (render [{:keys [color depth stencil]} {:keys [gl]}]
    (when-let [[r g b a] color]
      (.clearColor gl r g b a))
    (some->> depth (.clearDepth gl))
    (some->> stencil (.clearStencil gl))
    (->> [(when color gl.COLOR_BUFFER_BIT)
          (when depth gl.DEPTH_BUFFER_BIT)
          (when stencil gl.STENCIL_BUFFER_BIT)]
         (remove nil?)
         (apply bit-or)
         (.clear gl))))

(defrecord Viewport [x y width height])

(extend-type Viewport
  Renderable
  (render [{:keys [x y width height]} {:keys [gl]}]
    (.viewport gl x y width height)))

