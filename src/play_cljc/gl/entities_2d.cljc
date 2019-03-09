(ns play-cljc.gl.entities-2d
  (:require [play-cljc.gl.core :as c]
            [play-cljc.math :as m]
            [play-cljc.transforms :as t]
            [play-cljc.primitives-2d :as primitives]
            #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])))

(defrecord TwoDEntity [])

(extend-type TwoDEntity
  t/IProject
  (project [entity width height]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 3 (m/projection-matrix width height) %)))
  t/ITranslate
  (translate [entity x y]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 3 (m/translation-matrix x y) %)))
  t/IScale
  (scale [entity x y]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 3 (m/scaling-matrix x y) %)))
  t/IRotate
  (rotate [entity angle]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 3 (m/rotation-matrix angle) %)))
  t/IColor
  (color [entity rgba]
    (assoc-in entity [:uniforms 'u_color] rgba)))

(def ^:private two-d-vertex-shader
  {:attributes
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

(defn ->entity [game data]
  (->> {:vertex two-d-vertex-shader
        :fragment two-d-fragment-shader
        :attributes {'a_position {:data data
                                  :type (gl game FLOAT)
                                  :size 2}}}
       (c/->entity game)
       map->TwoDEntity))

(def ^:private image-vertex-shader
  {:attributes
   '{a_position vec2}
   :uniforms
   '{u_matrix mat3
     u_image sampler2D}
   :varyings
   '{v_texCoord vec2}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([]
           (= gl_Position
              (vec4
                (.xy (* u_matrix (vec3 a_position 1)))
                0 1))
           (= v_texCoord a_position))}})

(def ^:private image-fragment-shader
  {:precision "mediump float"
   :uniforms
   '{u_image sampler2D}
   :varyings
   '{v_texCoord vec2}
   :outputs
   '{outColor vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= outColor (.bgra (texture u_image v_texCoord))))}})

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
                                      (gl game NEAREST)}}}}
       (c/->entity game)
       map->TwoDEntity))

