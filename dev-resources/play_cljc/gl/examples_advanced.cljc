(ns play-cljc.gl.examples-advanced
  "Advanced 3D examples based on content from webgl2fundamentals.org"
  (:require [play-cljc.gl.core :as c]
            [play-cljc.gl.utils :as u]
            [play-cljc.gl.example-utils :as eu]
            [play-cljc.gl.example-data :as data]
            [play-cljc.primitives-3d :as primitives]
            [play-cljc.math :as m]
            #?(:clj  [play-cljc.macros-java :refer [gl math]]
               :cljs [play-cljc.macros-js :refer-macros [gl math]])
            #?(:clj [dynadoc.example :refer [defexample]]))
  #?(:cljs (:require-macros [dynadoc.example :refer [defexample]])))

(defn advanced-render [{:keys [entity objects total-time] :as game}]
  (eu/resize-example game)
  (c/render game
    {:clear {:color [1 1 1 1] :depth 1}})
  (let [[width height] (eu/get-size game)
        projection-matrix (m/perspective-matrix-3d (m/deg->rad 60)
                                                   (/ width height)
                                                   1
                                                   2000)
        camera-pos [0 0 100]
        target [0 0 0]
        up [0 1 0]
        camera-matrix (m/look-at-matrix-3d camera-pos target up)
        view-matrix (m/inverse-matrix-3d camera-matrix)
        view-projection-matrix (m/multiply-matrices-3d view-matrix projection-matrix)
        entity (assoc entity
                 :uniforms {'u_lightWorldPos [-50 30 100]
                            'u_viewInverse camera-matrix
                            'u_lightColor [1 1 1 1]})]
    (doseq [{:keys [rx ry tz mat-uniforms]}
            objects]
      (let [world-matrix (->> (m/identity-matrix 4)
                              (m/multiply-matrices-3d (m/x-rotation-matrix-3d (* rx total-time)))
                              (m/multiply-matrices-3d (m/y-rotation-matrix-3d (* ry total-time)))
                              (m/multiply-matrices-3d (m/translation-matrix-3d 0 0 tz)))]
        (c/render game
          (-> entity
              (assoc :viewport {:x 0 :y 0 :width width :height height})
              (update :uniforms assoc
                'u_world world-matrix
                'u_worldViewProjection (->> view-projection-matrix
                                            (m/multiply-matrices-3d world-matrix))
                'u_worldInverseTranspose (->> world-matrix
                                              (m/inverse-matrix-3d)
                                              (m/transpose-matrix-3d))
                'u_color (:u_color mat-uniforms)
                'u_specular (:u_specular mat-uniforms)
                'u_shininess (:u_shininess mat-uniforms)
                'u_specularFactor (:u_specularFactor mat-uniforms)))))))
  game)

(defn shape-entity [game {:keys [positions normals texcoords indices]}]
  (c/compile game
    {:vertex data/advanced-vertex-shader
     :fragment data/advanced-fragment-shader
     :attributes {'a_position {:data positions
                               :type (gl game FLOAT)
                               :size 3}
                  'a_normal {:data normals
                             :type (gl game FLOAT)
                             :size 3}
                  'a_texCoord {:data texcoords
                               :type (gl game FLOAT)
                               :size 2}}
     :indices {:data (#?(:clj short-array :cljs #(js/Uint16Array. %)) indices)
               :type (gl game UNSIGNED_SHORT)}}))

;; balls-3d

(defn balls-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (shape-entity game
                 (primitives/sphere {:radius 10 :subdivisions-axis 48 :subdivisions-height 24}))
        objects (vec
                  (for [i (range 50)]
                    {:tz (rand 150)
                     :rx (rand (* 2 (math PI)))
                     :ry (rand (math PI))
                     :mat-uniforms {:u_color           [(rand) (rand) (rand) 1]
                                    :u_specular        [1, 1, 1, 1]
                                    :u_shininess       (rand 500)
                                    :u_specularFactor  (rand 1)}}))]
    (assoc game :entity entity :objects objects)))

(defexample play-cljc.gl.examples-advanced/balls-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-advanced/balls-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-advanced/advanced-render)))

;; planes-3d

(defn planes-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (shape-entity game
                 (primitives/plane {:width 20 :depth 20 :subdivisions-width 10 :subdivisions-height 10}))
        objects (vec
                  (for [i (range 50)]
                    {:tz (rand 150)
                     :rx (rand (* 2 (math PI)))
                     :ry (rand (math PI))
                     :mat-uniforms {:u_color           [(rand) (rand) (rand) 1]
                                    :u_specular        [1, 1, 1, 1]
                                    :u_shininess       (rand 500)
                                    :u_specularFactor  (rand 1)}}))]
    (assoc game :entity entity :objects objects)))

(defexample play-cljc.gl.examples-advanced/planes-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-advanced/planes-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-advanced/advanced-render)))

;; cubes-3d

(defn cubes-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (shape-entity game
                 (primitives/cube {:size 20}))
        objects (vec
                  (for [i (range 50)]
                    {:tz (rand 150)
                     :rx (rand (* 2 (math PI)))
                     :ry (rand (math PI))
                     :mat-uniforms {:u_color           [(rand) (rand) (rand) 1]
                                    :u_specular        [1, 1, 1, 1]
                                    :u_shininess       (rand 500)
                                    :u_specularFactor  (rand 1)}}))]
    (assoc game :entity entity :objects objects)))

(defexample play-cljc.gl.examples-advanced/cubes-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-advanced/cubes-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-advanced/advanced-render)))

;; cylinder-3d

(defn cylinder-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (shape-entity game
                 (primitives/cylinder {:bottom-radius 10 :top-radius 10 :height 30
                                       :radial-subdivisions 10 :vertical-subdivisions 10}))
        objects (vec
                  (for [i (range 50)]
                    {:tz (rand 150)
                     :rx (rand (* 2 (math PI)))
                     :ry (rand (math PI))
                     :mat-uniforms {:u_color           [(rand) (rand) (rand) 1]
                                    :u_specular        [1, 1, 1, 1]
                                    :u_shininess       (rand 500)
                                    :u_specularFactor  (rand 1)}}))]
    (assoc game :entity entity :objects objects)))

(defexample play-cljc.gl.examples-advanced/cylinder-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-advanced/cylinder-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-advanced/advanced-render)))

;; crescent-3d

(defn crescent-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (shape-entity game
                 (primitives/crescent {:vertical-radius 20 :outer-radius 20 :inner-radius 15
                                       :thickness 10 :subdivisions-down 30}))
        objects (vec
                  (for [i (range 50)]
                    {:tz (rand 150)
                     :rx (rand (* 2 (math PI)))
                     :ry (rand (math PI))
                     :mat-uniforms {:u_color           [(rand) (rand) (rand) 1]
                                    :u_specular        [1, 1, 1, 1]
                                    :u_shininess       (rand 500)
                                    :u_specularFactor  (rand 1)}}))]
    (assoc game :entity entity :objects objects)))

(defexample play-cljc.gl.examples-advanced/crescent-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-advanced/crescent-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-advanced/advanced-render)))

;; torus-3d

(defn torus-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (shape-entity game
                 (primitives/torus {:radius 20 :thickness 5
                                    :radial-subdivisions 20 :body-subdivisions 20}))
        objects (vec
                  (for [i (range 50)]
                    {:tz (rand 150)
                     :rx (rand (* 2 (math PI)))
                     :ry (rand (math PI))
                     :mat-uniforms {:u_color           [(rand) (rand) (rand) 1]
                                    :u_specular        [1, 1, 1, 1]
                                    :u_shininess       (rand 500)
                                    :u_specularFactor  (rand 1)}}))]
    (assoc game :entity entity :objects objects)))

(defexample play-cljc.gl.examples-advanced/torus-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-advanced/torus-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-advanced/advanced-render)))

;; disc-3d

(defn disc-3d-init [game]
  (gl game enable (gl game CULL_FACE))
  (gl game enable (gl game DEPTH_TEST))
  (let [entity (shape-entity game
                 (primitives/disc {:radius 20 :divisions 20}))
        objects (vec
                  (for [i (range 50)]
                    {:tz (rand 150)
                     :rx (rand (* 2 (math PI)))
                     :ry (rand (math PI))
                     :mat-uniforms {:u_color           [(rand) (rand) (rand) 1]
                                    :u_specular        [1, 1, 1, 1]
                                    :u_shininess       (rand 500)
                                    :u_specularFactor  (rand 1)}}))]
    (assoc game :entity entity :objects objects)))

(defexample play-cljc.gl.examples-advanced/disc-3d
  {:with-card card}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-advanced/disc-3d-init)
       (play-cljc.gl.example-utils/game-loop
         play-cljc.gl.examples-advanced/advanced-render)))

