(ns play-cljc.gl.entities-2d
  (:require [play-cljc.math :as m]
            [play-cljc.transforms :as t]
            [play-cljc.instances :as i]
            [play-cljc.primitives-2d :as primitives]
            [play-cljc.gl.utils :as u]
            #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])))

(def ^:private ^:const reverse-matrix (m/scaling-matrix -1 -1))

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

;; Camera

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

;; InstancedTwoDEntity

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
   '{o_color vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= o_color v_color))}})

(def ^:private instanced-two-d-attrs->unis
  '{a_matrix u_matrix
    a_color u_color})

(defrecord InstancedTwoDEntity [instance-count])

(extend-type InstancedTwoDEntity
  i/IInstanced
  (assoc [instanced-entity i entity]
    (reduce-kv
      (partial u/assoc-instance-attr i entity)
      instanced-entity
      instanced-two-d-attrs->unis))
  (dissoc [instanced-entity i]
    (reduce-kv
      (partial u/dissoc-instance-attr i)
      instanced-entity
      instanced-two-d-attrs->unis)))

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
   '{o_color vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= o_color u_color))}})

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
  i/IInstance
  (->instanced-entity [entity]
    (-> entity
        (assoc :vertex instanced-two-d-vertex-shader
               :fragment instanced-two-d-fragment-shader)
        (update :uniforms dissoc 'u_matrix 'u_color)
        (update :attributes merge {'a_matrix {:data [] :divisor 1}
                                   'a_color {:data [] :divisor 1}})
        map->InstancedTwoDEntity)))

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
     u_texture_matrix mat3}
   :outputs
   '{v_tex_coord vec2}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([]
           (= gl_Position
              (vec4
                (.xy (* u_matrix (vec3 a_position 1)))
                0 1))
           (= v_tex_coord (.xy (* u_texture_matrix (vec3 a_position 1)))))}})

(def ^:private image-fragment-shader
  {:precision "mediump float"
   :uniforms
   '{u_image sampler2D}
   :inputs
   '{v_tex_coord vec2}
   :outputs
   '{o_color vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= o_color (texture u_image v_tex_coord)))}})

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
    (update-in entity [:uniforms 'u_texture_matrix]
      #(->> %
            (m/multiply-matrices 3
              (m/translation-matrix (/ crop-x width) (/ crop-y height)))
            (m/multiply-matrices 3
              (m/scaling-matrix (/ crop-width width) (/ crop-height height)))))))

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
                    'u_texture_matrix (m/identity-matrix 3)}
         :width width
         :height height}
        map->ImageEntity))

