(ns play-cljc.entities-3d
  (:require [play-cljc.core :as c]
            [play-cljc.math :as m]
            [play-cljc.transforms :as t]
            #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])))

(defrecord ThreeDEntity [])

(extend-type ThreeDEntity
  t/IProject
  (project [entity attrs]
    (let [matrix (cond
                   (every? attrs [:left :right :bottom :top :near :far])
                   (m/ortho-matrix-3d attrs)
                   (every? attrs [:field-of-view :aspect :near :far])
                   (m/perspective-matrix-3d attrs)
                   :else
                   (throw (ex-info "Can't project entity" entity)))]
      (update-in entity [:uniforms 'u_matrix]
        #(m/multiply-matrices 4 matrix %))))
  t/ITranslate
  (translate [entity {:keys [x y z]}]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 4 (m/translation-matrix-3d x y z) %)))
  t/IScale
  (scale [entity {:keys [x y z]}]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 4 (m/scaling-matrix-3d x y z) %)))
  t/IRotate
  (rotate [entity {:keys [angle axis]}]
    (let [matrix (case axis
                   :x (m/x-rotation-matrix-3d angle)
                   :y (m/y-rotation-matrix-3d angle)
                   :z (m/z-rotation-matrix-3d angle))]
      (update-in entity [:uniforms 'u_matrix]
        #(m/multiply-matrices 4 matrix %))))
  t/ICamera
  (camera [entity {:keys [matrix]}]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices 4 (m/inverse-matrix 4 matrix) %)))
  t/IColor
  (color [entity rgba]
    (assoc-in entity [:uniforms 'u_color] rgba)))

(defrecord Camera [])

(extend-type Camera
  t/ITranslate
  (translate [camera {:keys [x y z]}]
    (update camera :matrix
      #(m/multiply-matrices 4 (m/translation-matrix-3d x y z) %)))
  t/IRotate
  (rotate [camera {:keys [angle axis]}]
    (let [matrix (case axis
                   :x (m/x-rotation-matrix-3d angle)
                   :y (m/y-rotation-matrix-3d angle)
                   :z (m/z-rotation-matrix-3d angle))]
      (update camera :matrix
        #(m/multiply-matrices 4 matrix %))))
  t/ILookAt
  (look-at [{:keys [matrix] :as camera} {:keys [target up] :as attrs}]
    (let [camera-pos (if matrix
                       [(nth matrix 12)
                        (nth matrix 13)
                        (nth matrix 14)]
                       [0 0 0])]
      (when (= camera-pos target)
        (throw (ex-info "The camera's position is the same as the target" attrs)))
      (assoc camera :matrix (m/look-at camera-pos target up)))))

(def ^:private three-d-uniform-colors-vertex-shader
  {:attributes
   '{a_position vec4}
   :uniforms
   '{u_matrix mat4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([]
           (= gl_Position (* u_matrix a_position)))}})

(def ^:private three-d-uniform-colors-fragment-shader
  {:precision "mediump float"
   :uniforms
   '{u_color vec4}
   :outputs
   '{outColor vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= outColor u_color))}})

(def ^:private three-d-attribute-colors-vertex-shader
  {:attributes
   '{a_position vec4
     a_color vec4}
   :uniforms
   '{u_matrix mat4}
   :varyings
   '{v_color vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([]
           (= gl_Position (* u_matrix a_position))
           (= v_color a_color))}})

(def ^:private three-d-attribute-colors-fragment-shader
  {:precision "mediump float"
   :varyings
   '{v_color vec4}
   :outputs
   '{outColor vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= outColor v_color))}})

(defn ->entity
  ([game data]
   (->> {:vertex three-d-uniform-colors-vertex-shader
         :fragment three-d-uniform-colors-fragment-shader
         :attributes {'a_position {:data data
                                   :type (gl game FLOAT)
                                   :size 3}}}
        (c/->entity game)
        map->ThreeDEntity))
  ([game data color-data]
   (->> {:vertex three-d-attribute-colors-vertex-shader
         :fragment three-d-attribute-colors-fragment-shader
         :attributes {'a_position {:data data
                                   :type (gl game FLOAT)
                                   :size 3}
                      'a_color {:data color-data
                                :type (gl game FLOAT)
                                :size 3}}}
        (c/->entity game)
        map->ThreeDEntity)))

