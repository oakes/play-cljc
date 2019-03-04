(ns play-cljc.core
  (:require [iglu.core :as ig]
            [iglu.parse :as parse]
            [play-cljc.utils :as u]))

(defn create-game [context]
  {:tex-count (atom 0)
   :context context})

(defn- attribute-type->array-type [context attr-type]
  (condp = attr-type
    context.BYTE js/Int8Array
    context.SHORT js/Int16Array
    context.UNSIGNED_BYTE js/Uint8Array
    context.UNSIGNED_SHORT js/Uint16Array
    context.FLOAT js/Float32Array
    context.HALF_FLOAT js/Float32Array))

(defn- convert-type [context attr-name attr-type data]
  (if (vector? data)
    (let [arr-type (or (attribute-type->array-type context attr-type)
                       (throw (ex-info (str "The type for " attr-name " is invalid") {})))]
      (new arr-type data))
    data))

(defn- create-texture [{:keys [context tex-count]} m uni-loc {:keys [data params opts mipmap alignment]}]
  (let [unit (dec (swap! tex-count inc))
        texture (.createTexture context)]
    (.activeTexture context (+ context.TEXTURE0 unit))
    (.bindTexture context context.TEXTURE_2D texture)
    (doseq [[param-name param-val] params]
      (.texParameteri context context.TEXTURE_2D param-name param-val))
    (when alignment
      (.pixelStorei context context.UNPACK_ALIGNMENT alignment))
    (let [{:keys [mip-level internal-fmt src-fmt src-type width height border]} opts]
      (if (and width height border)
        (.texImage2D context context.TEXTURE_2D mip-level internal-fmt width height border src-fmt src-type data)
        (.texImage2D context context.TEXTURE_2D mip-level internal-fmt src-fmt src-type data)))
    (when mipmap
      (.generateMipmap context context.TEXTURE_2D))
    {:unit unit
     :texture texture
     :location uni-loc
     :framebuffer (when (nil? data)
                    (let [fb (.createFramebuffer context)
                          previous-framebuffer (.getParameter context context.FRAMEBUFFER_BINDING)]
                      (.bindFramebuffer context context.FRAMEBUFFER fb)
                      (.framebufferTexture2D context context.FRAMEBUFFER context.COLOR_ATTACHMENT0
                        context.TEXTURE_2D texture 0)
                      (.bindFramebuffer context context.FRAMEBUFFER previous-framebuffer)
                      fb))}))

(defn- call-uniform* [{:keys [context] :as game} m glsl-type uni-loc uni-name data]
  (case glsl-type
    float (.uniform1f context uni-loc data)
    vec2 (.uniform2fv context uni-loc data)
    vec3 (.uniform3fv context uni-loc data)
    vec4 (.uniform4fv context uni-loc data)
    mat2 (.uniformMatrix2fv context uni-loc false data)
    mat3 (.uniformMatrix3fv context uni-loc false data)
    mat4 (.uniformMatrix4fv context uni-loc false data)
    sampler2D (assoc-in m [:textures uni-name]
                (create-texture game m uni-loc (update data :data
                                                 (fn [d]
                                                   (convert-type context uni-name
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

(defn create-entity [{:keys [context] :as game}
                     {:keys [vertex fragment attributes uniforms indices] :as m}]
  (let [vertex-source (ig/iglu->glsl :vertex vertex)
        fragment-source (ig/iglu->glsl :fragment fragment)
        previous-program (.getParameter context context.CURRENT_PROGRAM)
        previous-vao (.getParameter context context.VERTEX_ARRAY_BINDING)
        program (u/create-program game vertex-source fragment-source)
        _ (.useProgram context program)
        vao (.createVertexArray context)
        _ (.bindVertexArray context vao)
        counts (mapv (fn [[attr-name {:keys [data type] :as opts}]]
                       (u/create-buffer game program (name attr-name)
                         (convert-type context attr-name type data)
                         opts))
                 attributes)
        index-count (some->> indices (u/create-index-buffer game))
        uniform-locations (reduce
                            (fn [m uniform]
                              (assoc m uniform
                                (.getUniformLocation context program (name uniform))))
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
    (.useProgram context previous-program)
    (.bindVertexArray context previous-vao)
    entity))

(defn- render-clear [context {:keys [color depth stencil]}]
  (when-let [[r g b a] color]
    (.clearColor context r g b a))
  (some->> depth (.clearDepth context))
  (some->> stencil (.clearStencil context))
  (->> [(when color context.COLOR_BUFFER_BIT)
        (when depth context.DEPTH_BUFFER_BIT)
        (when stencil context.STENCIL_BUFFER_BIT)]
       (remove nil?)
       (apply bit-or)
       (.clear context)))

(defn- render-viewport [context {:keys [x y width height]}]
  (.viewport context x y width height))

(defn render-entity [{:keys [context] :as game}
                     {:keys [program vao index-count uniforms indices
                             viewport clear render-to-texture]
                      :as entity}]
  (let [previous-program (.getParameter context context.CURRENT_PROGRAM)
        previous-vao (.getParameter context context.VERTEX_ARRAY_BINDING)]
    (.useProgram context program)
    (.bindVertexArray context vao)
    (let [{:keys [textures]} (reduce
                               (partial call-uniform game)
                               entity
                               uniforms)]
      (doseq [{:keys [unit location]} (vals textures)]
        (.uniform1i context location unit))
      (doseq [[texture-name inner-entity] render-to-texture
              :let [texture (get textures texture-name)]]
        (when-not texture
          (throw (ex-info (str "Can't find " texture-name) {})))
        (when-not (:framebuffer texture)
          (throw (ex-info (str texture-name " must have :data set to nil") {})))
        (let [previous-framebuffer (.getParameter context context.FRAMEBUFFER_BINDING)]
          (.bindFramebuffer context context.FRAMEBUFFER (:framebuffer texture))
          (render-entity game inner-entity)
          (.bindFramebuffer context context.FRAMEBUFFER previous-framebuffer))))
    (some->> viewport (render-viewport context))
    (some->> clear (render-clear context))
    (if indices
      (.drawElements context context.TRIANGLES index-count context.UNSIGNED_SHORT 0)
      (.drawArrays context context.TRIANGLES 0 index-count))
    (.useProgram context previous-program)
    (.bindVertexArray context previous-vao)))

