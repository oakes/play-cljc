(ns play-cljc.core
  (:require #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])
            [iglu.core :as ig]
            [iglu.parse :as parse]
            [play-cljc.utils :as u]))

(defn create-game [context]
  {:tex-count (atom 0)
   :context context})

(defn- attribute-type->constructor [game attr-type]
  (condp = attr-type
    (gl game BYTE)           #?(:clj int-array   :cljs #(js/Int8Array. %))
    (gl game SHORT)          #?(:clj short-array :cljs #(js/Int16Array. %))
    (gl game INT)            #?(:clj int-array   :cljs #(js/Int32Array. %))
    (gl game UNSIGNED_BYTE)  #?(:clj int-array   :cljs #(js/Uint8Array. %))
    (gl game UNSIGNED_SHORT) #?(:clj short-array :cljs #(js/Uint16Array. %))
    (gl game UNSIGNED_INT)   #?(:clj int-array   :cljs #(js/Uint32Array. %))
    (gl game FLOAT)          #?(:clj float-array :cljs #(js/Float32Array. %))
    (gl game HALF_FLOAT)     #?(:clj float-array :cljs #(js/Float32Array. %))))

(defn- convert-type [game attr-name attr-type data]
  (if (vector? data)
    (let [arr-con (or (attribute-type->constructor game attr-type)
                      (throw (ex-info (str "The type for " attr-name " is invalid") {})))]
      (arr-con data))
    data))

(defn- create-texture [{:keys [tex-count] :as game} m uni-loc
                       {:keys [data params opts mipmap alignment]}]
  (let [unit (dec (swap! tex-count inc))
        texture (gl game #?(:clj genTextures :cljs createTexture))]
    (gl game activeTexture (+ (gl game TEXTURE0) unit))
    (gl game bindTexture (gl game TEXTURE_2D) texture)
    (doseq [[param-name param-val] params]
      (gl game texParameteri (gl game TEXTURE_2D) param-name param-val))
    (when alignment
      (gl game pixelStorei (gl game UNPACK_ALIGNMENT) alignment))
    (let [{:keys [mip-level internal-fmt src-fmt src-type width height border]} opts]
      (gl game texImage2D (gl game TEXTURE_2D) mip-level internal-fmt
        width height border src-fmt src-type data))
    (when mipmap
      (gl game generateMipmap (gl game TEXTURE_2D)))
    {:unit unit
     :texture texture
     :location uni-loc
     :framebuffer (when (nil? data)
                    (let [fb (gl game #?(:clj genFramebuffers :cljs createFramebuffer))
                          previous-framebuffer (gl game #?(:clj getInteger :cljs getParameter)
                                                 (gl game FRAMEBUFFER_BINDING))]
                      (gl game bindFramebuffer (gl game FRAMEBUFFER) fb)
                      (gl game framebufferTexture2D (gl game FRAMEBUFFER)
                        (gl game COLOR_ATTACHMENT0)
                        (gl game TEXTURE_2D) texture 0)
                      (gl game bindFramebuffer (gl game FRAMEBUFFER) previous-framebuffer)
                      fb))}))

(defn- call-uniform* [game m glsl-type uni-loc uni-name data]
  (case glsl-type
    float     (gl game uniform1f uni-loc #?(:clj (float data) :cljs data))
    vec2      (gl game uniform2fv uni-loc #?(:clj (float-array data) :cljs data))
    vec3      (gl game uniform3fv uni-loc #?(:clj (float-array data) :cljs data))
    vec4      (gl game uniform4fv uni-loc #?(:clj (float-array data) :cljs data))
    mat2      (gl game uniformMatrix2fv uni-loc false #?(:clj (float-array data) :cljs data))
    mat3      (gl game uniformMatrix3fv uni-loc false #?(:clj (float-array data) :cljs data))
    mat4      (gl game uniformMatrix4fv uni-loc false #?(:clj (float-array data) :cljs data))
    sampler2D (assoc-in m [:textures uni-name]
                (create-texture game m uni-loc (update data :data
                                                 (fn [d]
                                                   (convert-type game uni-name
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

(def ^:private
  glsl-version #?(:clj "410" :cljs "300 es"))

(defn create-entity [game {:keys [vertex fragment attributes uniforms indices] :as m}]
  (let [vertex-source (ig/iglu->glsl :vertex (assoc vertex :version glsl-version))
        fragment-source (ig/iglu->glsl :fragment (assoc fragment :version glsl-version))
        previous-program (gl game #?(:clj getInteger :cljs getParameter)
                           (gl game CURRENT_PROGRAM))
        previous-vao (gl game #?(:clj getInteger :cljs getParameter)
                       (gl game VERTEX_ARRAY_BINDING))
        program (u/create-program game vertex-source fragment-source)
        _ (gl game useProgram program)
        vao (gl game #?(:clj genVertexArrays :cljs createVertexArray))
        _ (gl game bindVertexArray vao)
        counts (mapv (fn [[attr-name {:keys [data type] :as opts}]]
                       (u/create-buffer game program (name attr-name)
                         (convert-type game attr-name type data)
                         opts))
                 attributes)
        index-count (some->> indices
                             :data
                             ((or (attribute-type->constructor game (:type indices))
                                  (throw (ex-info "The :type provided to :indices is invalid" {}))))
                             (u/create-index-buffer game))
        uniform-locations (reduce
                            (fn [m uniform]
                              (assoc m uniform
                                (gl game getUniformLocation program (name uniform))))
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
    (gl game useProgram previous-program)
    (gl game bindVertexArray previous-vao)
    entity))

(defn- render-clear [game {:keys [color depth stencil]}]
  (when-let [[r g b a] color]
    (gl game clearColor r g b a))
  (some->> depth (gl game clearDepth))
  (some->> stencil (gl game clearStencil))
  (->> [(when color (gl game COLOR_BUFFER_BIT))
        (when depth (gl game DEPTH_BUFFER_BIT))
        (when stencil (gl game STENCIL_BUFFER_BIT))]
       (remove nil?)
       (apply bit-or)
       (gl game clear)))

(defn- render-viewport [game {:keys [x y width height]}]
  (gl game viewport x y width height))

(defn render-entity [game
                     {:keys [program vao index-count uniforms indices
                             viewport clear render-to-texture]
                      :as entity}]
  (let [previous-program (gl game #?(:clj getInteger :cljs getParameter)
                           (gl game CURRENT_PROGRAM))
        previous-vao (gl game #?(:clj getInteger :cljs getParameter)
                       (gl game VERTEX_ARRAY_BINDING))]
    (some->> program (gl game useProgram))
    (some->> vao (gl game bindVertexArray))
    (let [{:keys [textures]} (reduce
                               (partial call-uniform game)
                               entity
                               uniforms)]
      (doseq [{:keys [unit location]} (vals textures)]
        (gl game uniform1i location unit))
      (doseq [[texture-name inner-entity] render-to-texture
              :let [texture (get textures texture-name)]]
        (when-not texture
          (throw (ex-info (str "Can't find " texture-name) {})))
        (when-not (:framebuffer texture)
          (throw (ex-info (str texture-name " must have :data set to nil") {})))
        (let [previous-framebuffer (gl game #?(:clj getInteger :cljs getParameter)
                                     (gl game FRAMEBUFFER_BINDING))]
          (gl game bindFramebuffer (gl game FRAMEBUFFER) (:framebuffer texture))
          (render-entity game inner-entity)
          (gl game bindFramebuffer (gl game FRAMEBUFFER) previous-framebuffer))))
    (some->> viewport (render-viewport game))
    (some->> clear (render-clear game))
    (when index-count
      (if-let [{:keys [type]} indices]
        (gl game drawElements (gl game TRIANGLES) index-count type 0)
        (gl game drawArrays (gl game TRIANGLES) 0 index-count)))
    (gl game useProgram previous-program)
    (gl game bindVertexArray previous-vao)))

