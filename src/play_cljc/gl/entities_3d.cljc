(ns play-cljc.gl.entities-3d
  (:require [play-cljc.math :as m]
            [play-cljc.transforms :as t]
            #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])))

(defrecord ThreeDEntity [])

(extend-type ThreeDEntity
  t/IProject
  (project
    ([entity left right bottom top near far]
     (update-in entity [:uniforms 'u_matrix]
       #(m/multiply-matrices-3d (m/ortho-matrix-3d left right bottom top near far) %)))
    ([entity field-of-view aspect near far]
     (update-in entity [:uniforms 'u_matrix]
       #(m/multiply-matrices-3d (m/perspective-matrix-3d field-of-view aspect near far) %))))
  t/ITranslate
  (translate [entity x y z]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices-3d (m/translation-matrix-3d x y z) %)))
  t/IScale
  (scale [entity x y z]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices-3d (m/scaling-matrix-3d x y z) %)))
  t/IRotate
  (rotate [entity angle axis]
    (let [matrix (case axis
                   :x (m/x-rotation-matrix-3d angle)
                   :y (m/y-rotation-matrix-3d angle)
                   :z (m/z-rotation-matrix-3d angle))]
      (update-in entity [:uniforms 'u_matrix]
        #(m/multiply-matrices-3d matrix %))))
  t/ICamera
  (camera [entity cam]
    (t/invert entity cam))
  t/IInvert
  (invert [entity {:keys [matrix]}]
    (update-in entity [:uniforms 'u_matrix]
      #(m/multiply-matrices-3d (m/inverse-matrix-3d matrix) %)))
  t/IColor
  (color [entity rgba]
    (assoc-in entity [:uniforms 'u_color] rgba)))

(defrecord Camera [matrix])

(extend-type Camera
  t/ITranslate
  (translate [camera x y z]
    (update camera :matrix
      #(m/multiply-matrices-3d (m/translation-matrix-3d x y z) %)))
  t/IScale
  (scale [camera x y z]
    (update camera :matrix
      #(m/multiply-matrices-3d (m/scaling-matrix-3d x y z) %)))
  t/IRotate
  (rotate [camera angle axis]
    (let [matrix (case axis
                   :x (m/x-rotation-matrix-3d angle)
                   :y (m/y-rotation-matrix-3d angle)
                   :z (m/z-rotation-matrix-3d angle))]
      (update camera :matrix
        #(m/multiply-matrices-3d matrix %))))
  t/ILookAt
  (look-at [{:keys [matrix] :as camera} target up]
    (let [camera-pos [(nth matrix 12)
                      (nth matrix 13)
                      (nth matrix 14)]]
      (when (= camera-pos target)
        (throw (ex-info "The camera's position is the same as the target" {:target target})))
      (assoc camera :matrix (m/look-at-matrix-3d camera-pos target up)))))

(defn ->camera
  "Returns a 3D camera"
  []
  (->Camera (m/identity-matrix 4)))

(def ^:private three-d-uniform-colors-vertex-shader
  {:inputs
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
   '{o_color vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= o_color u_color))}})

(def ^:private three-d-attribute-colors-vertex-shader
  {:inputs
   '{a_position vec4
     a_color vec4}
   :uniforms
   '{u_matrix mat4}
   :outputs
   '{v_color vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([]
           (= gl_Position (* u_matrix a_position))
           (= v_color a_color))}})

(def ^:private three-d-attribute-colors-fragment-shader
  {:precision "mediump float"
   :inputs
   '{v_color vec4}
   :outputs
   '{o_color vec4}
   :signatures
   '{main ([] void)}
   :functions
   '{main ([] (= o_color v_color))}})

(defn ->entity
  "Returns a 3D entity with a vector of attribute `data` and optionally a vector of `color-data`."
  ([game data]
   (->> {:vertex three-d-uniform-colors-vertex-shader
         :fragment three-d-uniform-colors-fragment-shader
         :attributes {'a_position {:data data
                                   :type (gl game FLOAT)
                                   :size 3}}}
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
        map->ThreeDEntity)))

