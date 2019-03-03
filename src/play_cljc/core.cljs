(ns play-cljc.core
  (:require [iglu.core :as ig]
            [iglu.parse :as parse]
            [play-cljc.utils :as u]))

(defn create-game [gl]
  {:tex-count (atom 0)
   :gl gl})

(defn- attribute-type->array-type [gl attr-type]
  (condp = attr-type
    gl.BYTE js/Int8Array
    gl.SHORT js/Int16Array
    gl.UNSIGNED_BYTE js/Uint8Array
    gl.UNSIGNED_SHORT js/Uint16Array
    gl.FLOAT js/Float32Array
    gl.HALF_FLOAT js/Float32Array))

(defn- convert-type [gl attr-name attr-type data]
  (if (vector? data)
    (let [arr-type (or (attribute-type->array-type gl attr-type)
                       (throw (ex-info (str "The type for " attr-name " is invalid") {})))]
      (new arr-type data))
    data))

(defn- create-texture [{:keys [gl tex-count]} m uni-loc {:keys [data params opts mipmap alignment]}]
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
    {:unit unit
     :texture texture
     :location uni-loc
     :framebuffer (when (nil? data)
                    (let [fb (.createFramebuffer gl)]
                      (.bindFramebuffer gl gl.FRAMEBUFFER fb)
                      (.framebufferTexture2D gl gl.FRAMEBUFFER gl.COLOR_ATTACHMENT0
                        gl.TEXTURE_2D texture 0)
                      fb))}))

(defn- call-uniform* [{:keys [gl] :as game} m glsl-type uni-loc uni-name data]
  (case glsl-type
    float (.uniform1f gl uni-loc data)
    vec2 (.uniform2fv gl uni-loc data)
    vec3 (.uniform3fv gl uni-loc data)
    vec4 (.uniform4fv gl uni-loc data)
    mat2 (.uniformMatrix2fv gl uni-loc false data)
    mat3 (.uniformMatrix3fv gl uni-loc false data)
    mat4 (.uniformMatrix4fv gl uni-loc false data)
    sampler2D (assoc-in m [:textures uni-name]
                (create-texture game m uni-loc (update data :data
                                                 (fn [d]
                                                   (convert-type gl uni-name
                                                     (-> data :opts :src-type) d)))))))

(defn- get-uniform-type [{:keys [vertex fragment]} uni-name]
  (or (get-in vertex [:uniforms uni-name])
      (get-in fragment [:uniforms uni-name])
      (throw (ex-info (str "You must define " uni-name " in your vertex or fragment shader") {}))))

(defn- call-uniform [game {:keys [uniform-locations] :as m} [uni-name uni-data]]
  (let [uni-type (get-uniform-type m uni-name)
        uni-loc (get uniform-locations uni-name)]
    (or (call-uniform* game m uni-type uni-loc uni-name uni-data)
        m)))

(defn create-entity [{:keys [gl] :as game}
                     {:keys [vertex fragment attributes uniforms indices] :as m}]
  (let [vertex-source (ig/iglu->glsl :vertex vertex)
        fragment-source (ig/iglu->glsl :fragment fragment)
        program (u/create-program gl vertex-source fragment-source)
        _ (.useProgram gl program)
        vao (.createVertexArray gl)
        _ (.bindVertexArray gl vao)
        counts (mapv (fn [[attr-name {:keys [data type] :as opts}]]
                       (u/create-buffer gl program (name attr-name)
                         (convert-type gl attr-name type data)
                         opts))
                 attributes)
        index-count (some->> indices (u/create-index-buffer gl))
        uniform-locations (reduce
                            (fn [m uniform]
                              (assoc m uniform
                                (.getUniformLocation gl program (name uniform))))
                            {}
                            (-> #{}
                                (into (-> vertex :uniforms keys))
                                (into (-> fragment :uniforms keys))))
        entity (-> m
                   (dissoc :uniforms :attributes)
                   (merge {:vertex vertex
                           :fragment fragment
                           :vertex-source vertex-source
                           :fragment-source fragment-source
                           :program program
                           :vao vao
                           :uniform-locations uniform-locations
                           :textures {}
                           :index-count (or index-count (apply max counts))}))
        entity (reduce
                 (partial call-uniform game)
                 entity
                 uniforms)]
    (.bindVertexArray gl nil)
    entity))

(defn- render-clear [gl {:keys [color depth stencil]}]
  (when-let [[r g b a] color]
    (.clearColor gl r g b a))
  (some->> depth (.clearDepth gl))
  (some->> stencil (.clearStencil gl))
  (->> [(when color gl.COLOR_BUFFER_BIT)
        (when depth gl.DEPTH_BUFFER_BIT)
        (when stencil gl.STENCIL_BUFFER_BIT)]
       (remove nil?)
       (apply bit-or)
       (.clear gl)))

(defn- render-viewport [gl {:keys [x y width height]}]
  (.viewport gl x y width height))

(defn render-entity [{:keys [gl] :as game}
                     {:keys [program vao index-count uniforms indices
                             viewport clear render-to-texture]
                      :as entity}]
  (let [previous-program (.getParameter gl gl.CURRENT_PROGRAM)
        previous-vao (.getParameter gl gl.VERTEX_ARRAY_BINDING)]
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (let [{:keys [textures]} (reduce
                               (partial call-uniform game)
                               entity
                               uniforms)]
      (doseq [{:keys [unit location]} (vals textures)]
        (.uniform1i gl location unit))
      (doseq [[texture-name inner-entity] render-to-texture
              :let [texture (get textures texture-name)]]
        (when-not texture
          (throw (ex-info (str "Can't find " texture-name) {})))
        (when-not (:framebuffer texture)
          (throw (ex-info (str texture-name " must have :data set to nil") {})))
        (.bindFramebuffer gl gl.FRAMEBUFFER (:framebuffer texture))
        (render-entity game inner-entity)
        (.bindFramebuffer gl gl.FRAMEBUFFER nil)))
    (some->> viewport (render-viewport gl))
    (some->> clear (render-clear gl))
    (if indices
      (.drawElements gl gl.TRIANGLES index-count gl.UNSIGNED_SHORT 0)
      (.drawArrays gl gl.TRIANGLES 0 index-count))
    (.bindVertexArray gl previous-vao)
    (.useProgram gl previous-program)))

