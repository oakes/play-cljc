(ns play-cljc.gl.entities-2d
  (:require [play-cljc.math :as m]
            [play-cljc.transforms :as t]
            [play-cljc.primitives-2d :as primitives]
            [play-cljc.gl.entities-instanced :as ei]
            #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])))

(defrecord Camera [matrix])

(extend-type Camera
  t/ITranslate
  (translate [camera x y]
    (update camera :matrix
      #(m/multiply-matrices 3 (m/translation-matrix x y) %)))
  t/IRotate
  (rotate [camera angle]
    (update camera :matrix
      #(m/multiply-matrices 3 (m/rotation-matrix angle) %))))

(defn ->camera [y-down?]
  (->Camera (if y-down?
              (m/look-at-matrix [0 0 1] [0 -1 0])
              (m/look-at-matrix [0 0 -1] [0 1 0]))))

(def ^:private reverse-matrix (m/scaling-matrix -1 -1))

(defn- project [entity width height]
  (update-in entity [:uniforms 'u_matrix]
    #(m/multiply-matrices 3 (m/projection-matrix width height) %)))

(defn- translate [entity x y]
  (update-in entity [:uniforms 'u_matrix]
    #(m/multiply-matrices 3 (m/translation-matrix x y) %)))

(defn- scale [entity x y]
  (update-in entity [:uniforms 'u_matrix]
    #(m/multiply-matrices 3 (m/scaling-matrix x y) %)))

(defn- rotate [entity angle]
  (update-in entity [:uniforms 'u_matrix]
    #(m/multiply-matrices 3 (m/rotation-matrix angle) %)))

(defn- camera [entity {:keys [matrix]}]
  (update-in entity [:uniforms 'u_matrix]
    #(->> %
          (m/multiply-matrices 3 matrix)
          (m/multiply-matrices 3 reverse-matrix))))

;; TwoDEntity

(def ^:private two-d-vertex-shader
  {:inputs
   '{a_position vec2}
   :uniforms
   '{u_matrix mat3}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([]
           (= gl_Position
              (vec4
                (.xy (* u_matrix (vec3 a_position 1)))
                0 1)))}})

(def ^:private two-d-fragment-shader
  {:precision "mediump float"
   :uniforms
   '{u_color vec4}
   :outputs
   '{outColor vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= outColor u_color))}})

(def ^:private instanced-two-d-vertex-shader
  {:inputs
   '{a_position vec2
     a_matrix mat3
     a_color vec4}
   :outputs
   '{v_color vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([]
           (= v_color a_color)
           (= gl_Position
              (vec4
                (.xy (* a_matrix (vec3 a_position 1)))
                0 1)))}})

(def ^:private instanced-two-d-fragment-shader
  {:precision "mediump float"
   :inputs
   '{v_color vec4}
   :outputs
   '{outColor vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= outColor v_color))}})

(defrecord TwoDEntity [])

(extend-type TwoDEntity
  t/IProject
  (project [entity width height] (project entity width height))
  t/ITranslate
  (translate [entity x y] (translate entity x y))
  t/IScale
  (scale [entity x y] (scale entity x y))
  t/IRotate
  (rotate [entity angle] (rotate entity angle))
  t/ICamera
  (camera [entity cam] (camera entity cam))
  t/IColor
  (color [entity rgba]
    (assoc-in entity [:uniforms 'u_color] rgba))
  ei/ICreateInstancedEntity
  (->instanced-entity [entity]
    (-> entity
        (assoc :vertex instanced-two-d-vertex-shader
               :fragment instanced-two-d-fragment-shader)
        (dissoc :uniforms)
        ei/map->InstancedEntity))
  ei/IConjInstance
  (conj-instance [entity instanced-entity]
    (reduce-kv
      (fn [instanced-entity attr-name uni-name]
        (let [data (get-in entity [:uniforms uni-name])]
          (update-in instanced-entity [:attributes attr-name]
                     (fn [attr]
                       (if attr
                         (update attr :data into data)
                         {:data (vec data)
                          :divisor 1})))))
      instanced-entity
      '{a_matrix u_matrix
        a_color u_color})))

(defn ->entity [game data]
  (->> {:vertex two-d-vertex-shader
        :fragment two-d-fragment-shader
        :attributes {'a_position {:data data
                                  :type (gl game FLOAT)
                                  :size 2}}}
       map->TwoDEntity))

;; ImageEntity

(def ^:private image-vertex-shader
  {:inputs
   '{a_position vec2}
   :uniforms
   '{u_matrix mat3
     u_textureMatrix mat3}
   :outputs
   '{v_texCoord vec2}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([]
           (= gl_Position
              (vec4
                (.xy (* u_matrix (vec3 a_position 1)))
                0 1))
           (= v_texCoord (.xy (* u_textureMatrix (vec3 a_position 1)))))}})

(def ^:private image-fragment-shader
  {:precision "mediump float"
   :uniforms
   '{u_image sampler2D}
   :inputs
   '{v_texCoord vec2}
   :outputs
   '{outColor vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= outColor (texture u_image v_texCoord)))}})

(def ^:private instanced-image-vertex-shader
  {:inputs
   '{a_position vec2
     a_matrix mat3
     a_textureMatrix mat3}
   :outputs
   '{v_texCoord vec2}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([]
           (= gl_Position
              (vec4
                (.xy (* a_matrix (vec3 a_position 1)))
                0 1))
           (= v_texCoord (.xy (* a_textureMatrix (vec3 a_position 1)))))}})

(def ^:private instanced-image-fragment-shader
  {:precision "mediump float"
   :uniforms
   '{u_image sampler2D}
   :inputs
   '{v_texCoord vec2}
   :outputs
   '{outColor vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= outColor (texture u_image v_texCoord)))}})

(defrecord ImageEntity [width height])

(extend-type ImageEntity
  t/IProject
  (project [entity width height] (project entity width height))
  t/ITranslate
  (translate [entity x y] (translate entity x y))
  t/IScale
  (scale [entity x y] (scale entity x y))
  t/IRotate
  (rotate [entity angle] (rotate entity angle))
  t/ICamera
  (camera [entity cam] (camera entity cam))
  t/ICrop
  (crop [{:keys [width height] :as entity} crop-x crop-y crop-width crop-height]
    (update-in entity [:uniforms 'u_textureMatrix]
      #(->> %
            (m/multiply-matrices 3
              (m/translation-matrix (/ crop-x width) (/ crop-y height)))
            (m/multiply-matrices 3
              (m/scaling-matrix (/ crop-width width) (/ crop-height height))))))
  ei/ICreateInstancedEntity
  (->instanced-entity [entity]
    (-> entity
        (assoc :vertex instanced-image-vertex-shader
               :fragment instanced-image-fragment-shader)
        (update :uniforms dissoc 'u_matrix 'u_textureMatrix)
        ei/map->InstancedEntity))
  ei/IConjInstance
  (conj-instance [entity instanced-entity]
    (reduce-kv
      (fn [instanced-entity attr-name uni-name]
        (let [data (get-in entity [:uniforms uni-name])]
          (update-in instanced-entity [:attributes attr-name]
                     (fn [attr]
                       (if attr
                         (update attr :data into data)
                         {:data (vec data)
                          :divisor 1})))))
      instanced-entity
      '{a_matrix u_matrix
        a_textureMatrix u_textureMatrix})))

(defn ->image-entity [game data width height]
   (->> {:vertex image-vertex-shader
         :fragment image-fragment-shader
         :attributes {'a_position {:data primitives/rect
                                   :type (gl game FLOAT)
                                   :size 2}}
         :uniforms {'u_image {:data data
                              :opts {:mip-level 0
                                     :internal-fmt (gl game RGBA)
                                     :width width
                                     :height height
                                     :border 0
                                     :src-fmt (gl game RGBA)
                                     :src-type (gl game UNSIGNED_BYTE)}
                              :params {(gl game TEXTURE_WRAP_S)
                                       (gl game CLAMP_TO_EDGE),
                                       (gl game TEXTURE_WRAP_T)
                                       (gl game CLAMP_TO_EDGE),
                                       (gl game TEXTURE_MIN_FILTER)
                                       (gl game NEAREST),
                                       (gl game TEXTURE_MAG_FILTER)
                                       (gl game NEAREST)}}
                    'u_textureMatrix (m/identity-matrix 3)}
         :width width
         :height height}
        map->ImageEntity))

