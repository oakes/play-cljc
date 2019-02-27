(ns play-cljc.core
  (:require [iglu.core :as ig]
            [iglu.parse :as parse]
            [play-cljc.utils :as u]
            [clojure.string :as str]))

(defn glsl-type->platform-type [glsl-type]
  (let [s (name glsl-type)]
    (cond
      (str/starts-with? s "vec")  js/Float32Array
      (str/starts-with? s "dvec") js/Float64Array
      (str/starts-with? s "ivec") js/Int32Array
      (str/starts-with? s "uvec") js/Uint32Array)))

(defn call-uniform-fn [gl glsl-type uni-loc data]
  (case glsl-type
    vec2 (.uniform2fv gl uni-loc data)
    vec3 (.uniform3fv gl uni-loc data)
    vec4 (.uniform4fv gl uni-loc data)
    mat2 (.uniformMatrix2fv gl uni-loc false data)
    mat3 (.uniformMatrix3fv gl uni-loc false data)
    mat4 (.uniformMatrix4fv gl uni-loc false data)))

(defn create-entity [gl {:keys [vertex fragment attributes] :as m}]
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
                           (->> data clj->js (new attr-type))
                           opts)))
                 attributes)
        _ (.bindVertexArray gl nil)]
    (assoc m
      :vertex-source vertex-source
      :fragment-source fragment-source
      :program program
      :vao vao
      :uniform-locations (reduce
                           (fn [m uniform]
                             (assoc m uniform
                               (.getUniformLocation gl program (name uniform))))
                           {}
                           (-> #{}
                               (into (-> vertex :uniforms keys))
                               (into (-> fragment :uniforms keys))))
      :count (apply max counts))))

(defn render-entity [gl
                     {:keys [vertex fragment program vao uniform-locations count]}
                     {:keys [uniforms]}]
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (doseq [[uni-name uni-data] uniforms]
    (let [uni-type (or (get-in vertex [:uniforms uni-name])
                       (get-in fragment [:uniforms uni-name])
                       (parse/throw-error
                         (str "You must define " uni-name
                           " in your vertex or fragment shader")))
          uni-loc (get uniform-locations uni-name)]
      (call-uniform-fn gl uni-type uni-loc uni-data)))
  (.drawArrays gl gl.TRIANGLES 0 count)
  (.bindVertexArray gl nil))

