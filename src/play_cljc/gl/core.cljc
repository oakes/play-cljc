(ns play-cljc.gl.core
  (:require #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])
            [iglu.core :as ig]
            [iglu.parse :as parse]
            [play-cljc.gl.utils :as u]
            [clojure.spec.alpha :as s])
  (:refer-clojure :exclude [compile]))

(s/def ::tex-count #(instance? #?(:clj clojure.lang.Atom :cljs cljs.core/Atom) %))
(s/def ::context #?(:clj integer? :cljs #(instance? js/WebGL2RenderingContext %)))
(s/def ::game (s/keys :req-un [::tex-count ::context]))

(s/fdef ->game
  :args (s/cat :context ::context)
  :ret ::game)

(defn ->game [context]
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
    (gl game HALF_FLOAT)     #?(:clj float-array :cljs #(js/Float32Array. %))
    nil))

(defn- convert-type [game attr-name attr-type data]
  (if (vector? data)
    (let [arr-con (or (attribute-type->constructor game attr-type)
                      (throw (ex-info (str "The type for " attr-name " is invalid") {})))]
      (arr-con data))
    data))

(s/def ::unit integer?)
(s/def ::texture #?(:clj integer? :cljs #(instance? js/WebGLTexture %)))
(s/def ::location #?(:clj integer? :cljs #(instance? js/WebGLUniformLocation %)))
(s/def ::framebuffer (s/nilable #?(:clj integer? :cljs #(instance? js/WebGLFramebuffer %))))
(s/def ::texture-map (s/keys :req-un [::unit ::texture ::location ::framebuffer]))

(s/def ::data (s/or
                :vector vector?
                ;; TODO: make spec for java primitive arrays and JS typed arrays
                :array any?))

(s/def ::mip-level integer?)
(s/def ::internal-fmt integer?)
(s/def ::width number?)
(s/def ::height number?)
(s/def ::src-fmt integer?)
(s/def ::src-type integer?)
(s/def ::opts (s/keys :req-un [::mip-level ::internal-fmt ::width ::height ::src-fmt ::src-type]))

(s/def ::params (s/map-of integer? integer?))
(s/def ::mipmap boolean?)
(s/def ::alignment integer?)

(s/def ::texture-uniform (s/keys
                           :req-un [::opts]
                           :opt-un [::data ::params ::mipmap ::alignment]))

(defn- create-texture [{:keys [tex-count] :as game} uni-loc
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
      (gl game texImage2D (gl game TEXTURE_2D) (int mip-level) (int internal-fmt)
        (int width) (int height) (int border) (int src-fmt) (int src-type) data))
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

(def ^:private type->attribute-opts
  '{float {:size 1}
    vec2  {:size 2}
    vec3  {:size 3}
    vec4  {:size 4}
    mat2  {:size 4}
    mat3  {:size 3
           :iter 3}
    mat4  {:size 4
           :iter 4}})

(defn- get-uniform-type [{:keys [vertex fragment]} uni-name]
  (or (get-in vertex [:uniforms uni-name])
      (get-in fragment [:uniforms uni-name])
      (throw (ex-info (str "You must define " uni-name " in your vertex or fragment shader's :uniforms") {}))))

(defn- get-attribute-type [{:keys [vertex]} attr-name]
  (or (get-in vertex [:inputs attr-name])
      ;; for backwards compatibility
      (get-in vertex [:attributes attr-name])
      (throw (ex-info (str "You must define " attr-name " in your vertex shader's :inputs") {}))))

(defn- get-attribute-names [vertex]
  (or (some-> vertex :inputs keys)
      ;; for backwards compatibility
      (some-> vertex :attributes keys)))

(defn- call-uniform* [game entity glsl-type ^Integer uni-loc uni-name data]
  (case glsl-type
    float     (gl game uniform1f uni-loc #?(:clj (float data) :cljs data))
    vec2      (gl game uniform2fv uni-loc #?(:clj (float-array data) :cljs data))
    vec3      (gl game uniform3fv uni-loc #?(:clj (float-array data) :cljs data))
    vec4      (gl game uniform4fv uni-loc #?(:clj (float-array data) :cljs data))
    mat2      (gl game uniformMatrix2fv uni-loc false #?(:clj (float-array data) :cljs data))
    mat3      (gl game uniformMatrix3fv uni-loc false #?(:clj (float-array data) :cljs data))
    mat4      (gl game uniformMatrix4fv uni-loc false #?(:clj (float-array data) :cljs data))
    sampler2D (assoc-in entity [:textures uni-name]
                (create-texture game uni-loc (update data :data
                                               (fn [d]
                                                 (convert-type game uni-name
                                                   (-> data :opts :src-type) d)))))))

(defn- call-uniform [game {:keys [uniform-locations] :as entity} [uni-name uni-data]]
  (let [uni-type (get-uniform-type entity uni-name)
        uni-loc (get uniform-locations uni-name)]
    (or (call-uniform* game entity uni-type uni-loc uni-name uni-data)
        entity)))

(defn- merge-attribute-opts [game entity attr-name opts]
  (let [type-name (get-attribute-type entity attr-name)]
    (merge u/default-opts
           (type->attribute-opts type-name)
           (update opts :type #(or % (gl game FLOAT))))))

(defn- set-attribute [game entity program buffer attr-name {:keys [data type] :as opts}]
  (let [data (convert-type game attr-name type data)]
    (u/set-array-buffer game program buffer (name attr-name) data opts)))

(defn- set-buffer [game entity program m attr-name opts]
  (let [buffer (or (get-in entity [:attribute-buffers attr-name])
                   (throw (ex-info (str "Can't find buffer for attribute " attr-name) {})))
        opts (merge-attribute-opts game entity attr-name opts)
        divisor (:divisor opts)
        expected-count (get m divisor)
        draw-count (set-attribute game entity program buffer attr-name opts)]
    (when (and expected-count (not= expected-count draw-count))
      (throw (ex-info (str "The data in :attributes has an inconsistent size")
                      {:divisor divisor})))
    (assoc m divisor draw-count)))

(defn- set-buffers [game entity program]
  (let [divisor->draw-count (reduce-kv
                              (partial set-buffer game entity program)
                              {}
                              (:attributes entity))
        vertex-count (divisor->draw-count 0)
        instance-count (divisor->draw-count 1)]
    (if-let [{:keys [data type]} (:indices entity)]
      (let [ctor (or (attribute-type->constructor game type)
                     (throw (ex-info "The :type provided to :indices is invalid" {})))
            buffer (:index-buffer entity)
            draw-count (u/set-index-buffer game buffer (ctor data))]
        (assoc entity :draw-count draw-count))
      (cond-> entity
              vertex-count (assoc :draw-count vertex-count)
              instance-count (assoc :instance-count instance-count)))))

(declare render)

(defn- render->texture [game textures render-to-texture]
  (doseq [[texture-name inner-entities] render-to-texture
          :let [texture (get textures texture-name)]]
    (when-not texture
      (throw (ex-info (str "Can't find " texture-name) {})))
    (when-not (:framebuffer texture)
      (throw (ex-info (str texture-name " must have :data set to nil") {})))
    (let [previous-framebuffer (gl game #?(:clj getInteger :cljs getParameter)
                                 (gl game FRAMEBUFFER_BINDING))]
      (gl game bindFramebuffer (gl game FRAMEBUFFER) (:framebuffer texture))
      (if (map? inner-entities)
        (render game inner-entities)
        (run! #(render game %) inner-entities))
      (gl game bindFramebuffer (gl game FRAMEBUFFER) previous-framebuffer))))

(def ^:private
  glsl-version #?(:clj "410" :cljs "300 es"))

(s/def ::vertex ::parse/shader)
(s/def ::fragment ::parse/shader)
(s/def ::type integer?)
(s/def ::iter (s/and integer? pos?))
(s/def ::normalize boolean?)
(s/def ::stride (s/and integer? #(>= % 0)))
(s/def ::offset (s/and integer? #(>= % 0)))
(s/def ::divisor (s/and integer? #(>= % 0)))
(s/def ::attribute (s/keys :req-un [::data]
                           :opt-un [::type ::iter ::normalize ::stride ::offset ::divisor]))
(s/def ::attributes (s/map-of symbol? ::attribute))
(s/def ::uniforms (s/map-of symbol? (s/or
                                      :texture-uniform ::texture-uniform
                                      :uniform ::data)))
(s/def ::indices (s/keys :req-un [::data ::type]))

(s/def ::render-to-texture (s/map-of symbol? (s/or
                                               :single ::renderable
                                               :multiple (s/coll-of ::renderable))))

(s/def ::uncompiled-entity
  (s/keys
    :req-un [::vertex ::fragment]
    :opt-un [::attributes ::uniforms ::indices ::render-to-texture]))

(s/def ::program #?(:clj integer? :cljs #(instance? js/WebGLProgram %)))
(s/def ::vao #?(:clj integer? :cljs #(instance? js/WebGLVertexArrayObject %)))
(s/def ::uniform-locations (s/map-of symbol? ::location))
(s/def ::textures (s/map-of symbol? ::texture-map))
(s/def ::draw-count integer?)
(s/def ::instance-count integer?)
(s/def ::index-buffer #?(:clj integer? :cljs #(instance? js/WebGLBuffer %)))

(s/def ::compiled-entity
  (s/keys
    :req-un [::program ::vao ::uniform-locations ::textures]
    :opt-un [::render-to-texture ::draw-count ::instance-count ::index-buffer]))

(s/fdef compile
  :args (s/cat :game ::game :entity ::uncompiled-entity)
  :ret ::compiled-entity)

(defn compile
  "Initializes the provided entity, compiling the shaders and creating all the
  necessary state for rendering."
  [game {:keys [vertex fragment attributes uniforms indices] :as entity}]
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
        attr-names (get-attribute-names vertex)
        entity (cond-> entity
                       attr-names
                       (assoc :attribute-buffers
                         (reduce
                           (fn [m attr-name]
                             (assoc m attr-name (u/create-buffer game)))
                           {}
                           attr-names))
                       indices
                       (assoc :index-buffer (u/create-buffer game)))
        entity (set-buffers game entity program)
        uniform-locations (reduce
                            (fn [m uniform]
                              (assoc m uniform
                                (gl game getUniformLocation program (name uniform))))
                            {}
                            (-> #{}
                                (into (-> vertex :uniforms keys))
                                (into (-> fragment :uniforms keys))))
        entity (merge entity {:vertex vertex
                              :fragment fragment
                              :vertex-source vertex-source
                              :fragment-source fragment-source
                              :program program
                              :vao vao
                              :uniform-locations uniform-locations
                              :textures {}})
        entity (reduce
                 (partial call-uniform game)
                 entity
                 uniforms)]
    (some->> entity :render-to-texture (render->texture game (:textures entity)))
    (gl game useProgram previous-program)
    (gl game bindVertexArray previous-vao)
    (dissoc entity :uniforms :attributes :render-to-texture)))

(s/def ::zero-to-one #(<= 0 % 1))
(s/def ::color (s/tuple ::zero-to-one ::zero-to-one ::zero-to-one ::zero-to-one))
(s/def ::depth ::zero-to-one)
(s/def ::stencil integer?)
(s/def ::clear (s/keys :opt-un [::color ::depth ::stencil]))

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

(s/def ::x number?)
(s/def ::y number?)
(s/def ::viewport (s/keys :req-un [::x ::y ::width ::height]))

(defn- render-viewport [game {:keys [x y width height]}]
  (gl game viewport x y width height))

(s/def ::misc-map (s/keys :opt-un [::viewport ::clear]))
(s/def ::renderable (s/or
                      :compiled-entity (s/merge ::compiled-entity ::misc-map)
                      :misc-map ::misc-map))

(s/fdef render
  :args (s/cat
          :game ::game
          :entity ::renderable))

(defn render
  "Renders the provided entity."
  [game
   {:keys [program vao uniforms indices
           viewport clear render-to-texture index-buffer]
    :as entity}]
  (let [previous-program (gl game #?(:clj getInteger :cljs getParameter)
                           (gl game CURRENT_PROGRAM))
        previous-vao (gl game #?(:clj getInteger :cljs getParameter)
                       (gl game VERTEX_ARRAY_BINDING))
        previous-index-buffer (gl game #?(:clj getInteger :cljs getParameter)
                                (gl game ELEMENT_ARRAY_BUFFER_BINDING))]
    (some->> program (gl game useProgram))
    (some->> vao (gl game bindVertexArray))
    (some->> index-buffer (gl game bindBuffer (gl game ELEMENT_ARRAY_BUFFER)))
    (let [{:keys [textures]} (reduce
                               (partial call-uniform game)
                               entity
                               uniforms)]
      (doseq [{:keys [unit location]} (vals textures)]
        (gl game uniform1i location unit))
      (some->> entity :render-to-texture (render->texture game textures)))
    (some->> viewport (render-viewport game))
    (some->> clear (render-clear game))
    (let [{:keys [draw-count instance-count]} (set-buffers game entity program)]
      (when draw-count
        (if-let [{:keys [type]} indices]
          (gl game drawElements (gl game TRIANGLES) draw-count type 0)
          (if instance-count
            (gl game drawArraysInstanced (gl game TRIANGLES) 0 draw-count instance-count)
            (gl game drawArrays (gl game TRIANGLES) 0 draw-count)))))
    (gl game useProgram previous-program)
    (gl game bindVertexArray previous-vao)
    (gl game bindBuffer (gl game ELEMENT_ARRAY_BUFFER) previous-index-buffer)))

