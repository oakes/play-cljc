(ns play-cljc.gl.examples-2d
  "2D examples based on content from webgl2fundamentals.org"
  (:require [play-cljc.gl.core :as c]
            [play-cljc.gl.entities-2d :as e]
            [play-cljc.gl.entities-instanced :as ei]
            [play-cljc.gl.example-utils :as eu]
            [play-cljc.gl.example-data :as data]
            [play-cljc.transforms :as t]
            [play-cljc.primitives-2d :as primitives]
            #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])
            #?(:clj [dynadoc.example :refer [defexample]]))
  #?(:cljs (:require-macros [dynadoc.example :refer [defexample]])))

;; rand-rects

(defn rand-rects-example [game entity entities]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (->> entities
       (reduce ei/conj (ei/->instanced-entity entity))
       (c/compile game)
       (assoc game :entity)))

(defexample rand-rects-example
  {:with-card card
   :with-focus [focus (for [_ (range 50)]
                        (-> entity
                            (play-cljc.transforms/color [(rand) (rand) (rand) 1])
                            (play-cljc.transforms/translate (rand-int game-width) (rand-int game-height))
                            (play-cljc.transforms/scale (rand-int 300) (rand-int 300))))]}
  (let [game (play-cljc.gl.example-utils/init-example card)
        game-width (play-cljc.gl.example-utils/get-width game)
        game-height (play-cljc.gl.example-utils/get-height game)
        entity (-> (play-cljc.gl.entities-2d/->entity game play-cljc.primitives-2d/rect)
                   (assoc :viewport {:x 0 :y 0 :width game-width :height game-height})
                   (play-cljc.transforms/project game-width game-height))]
    (->> focus
         vec
         (play-cljc.gl.examples-2d/rand-rects-example game entity)
         (play-cljc.gl.example-utils/game-loop
           (fn rand-rects-render [{:keys [entity] :as game}]
             (play-cljc.gl.example-utils/resize-example game)
             (play-cljc.gl.core/render game
               {:clear {:color [1 1 1 1] :depth 1}})
             (play-cljc.gl.core/render game entity)
             game)))))

;; image

(defn image-example [game {:keys [data width height] :as image}]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (assoc game
    :entity
    (-> (c/compile game (e/->image-entity game data width height))
        (assoc :clear {:color [1 1 1 1] :depth 1}))
    :image
    image))

(defexample image-example
  {:with-card card
   :with-focus [focus (play-cljc.gl.core/render game
                        (-> entity
                            (assoc :viewport {:x 0 :y 0 :width game-width :height game-height})
                            (play-cljc.transforms/project game-width game-height)
                            (play-cljc.transforms/translate 0 0)
                            (play-cljc.transforms/scale img-width img-height)))]}
  (let [game (play-cljc.gl.example-utils/init-example card)]
    (play-cljc.gl.example-utils/get-image "aintgottaexplainshit.jpg"
      (fn [image]
        (->> (play-cljc.gl.examples-2d/image-example game image)
             (play-cljc.gl.example-utils/game-loop
               (fn image-render [{:keys [entity image] :as game}]
                 (play-cljc.gl.example-utils/resize-example game)
                 (let [game-width (play-cljc.gl.example-utils/get-width game)
                       game-height (play-cljc.gl.example-utils/get-height game)
                       screen-ratio (/ game-width game-height)
                       image-ratio (/ (:width image) (:height image))
                       [img-width img-height] (if (> screen-ratio image-ratio)
                                                [(* game-height (/ (:width image) (:height image))) game-height]
                                                [game-width (* game-width (/ (:height image) (:width image)))])]
                   focus)
                 game)))))))

;; translation

(defn translation-example [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (-> (c/compile game (e/->entity game data/f-2d))
                   (assoc :clear {:color [0 0 0 0] :depth 1}))
        *state (atom {:x 0 :y 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample translation-example
  {:with-card card
   :with-focus [focus (play-cljc.gl.core/render game
                        (-> (assoc entity :viewport {:x 0 :y 0
                                                     :width game-width
                                                     :height game-height})
                            (play-cljc.transforms/project game-width game-height)
                            (play-cljc.transforms/translate x y)
                            (play-cljc.transforms/color [1 0 0.5 1])))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-2d/translation-example)
       (play-cljc.gl.example-utils/game-loop
         (fn translation-render [{:keys [entity *state] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (let [{:keys [x y]} @*state]
             (let [game-width (play-cljc.gl.example-utils/get-width game)
                   game-height (play-cljc.gl.example-utils/get-height game)]
               focus))
           game))))

;; rotation

(defn rotation-example [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (-> (c/compile game (e/->entity game data/f-2d))
                   (assoc :clear {:color [0 0 0 0] :depth 1}))
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample rotation-example
  {:with-card card
   :with-focus [focus (play-cljc.gl.core/render game
                        (-> entity
                            (assoc :viewport {:x 0 :y 0 :width game-width :height game-height})
                            (play-cljc.transforms/project game-width game-height)
                            (play-cljc.transforms/translate tx ty)
                            (play-cljc.transforms/rotate r)
                            (play-cljc.transforms/color [1 0 0.5 1])
                            (play-cljc.transforms/translate -50 -75)))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-2d/rotation-example)
       (play-cljc.gl.example-utils/game-loop
         (fn rotation-render [{:keys [entity *state] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (let [{:keys [tx ty r]} @*state
                 game-width (play-cljc.gl.example-utils/get-width game)
                 game-height (play-cljc.gl.example-utils/get-height game)]
             focus)
           game))))

;; scale

(defn scale-example [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (-> (c/compile game (e/->entity game data/f-2d))
                   (assoc :clear {:color [0 0 0 0] :depth 1}))
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :rx 1 :ry 1})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample scale-example
  {:with-card card
   :with-focus [focus (play-cljc.gl.core/render game
                        (-> (assoc entity :viewport {:x 0 :y 0
                                                     :width game-width
                                                     :height game-height})
                            (play-cljc.transforms/project game-width game-height)
                            (play-cljc.transforms/translate tx ty)
                            (play-cljc.transforms/rotate 0)
                            (play-cljc.transforms/scale rx ry)
                            (play-cljc.transforms/color [1 0 0.5 1])))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-2d/scale-example)
       (play-cljc.gl.example-utils/game-loop
         (fn scale-render [{:keys [entity *state] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (let [{:keys [tx ty rx ry]} @*state
                 game-width (play-cljc.gl.example-utils/get-width game)
                 game-height (play-cljc.gl.example-utils/get-height game)]
             focus)
           game))))

;; rotation-multi

(defn rotation-multi-example [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (c/compile game (e/->entity game data/f-2d))
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse game *state)
    (assoc game :entity entity :*state *state)))

(defexample rotation-multi-example
  {:with-card card
   :with-focus [focus (loop [i 0
                             entity (-> entity
                                        (assoc :viewport {:x 0 :y 0
                                                          :width game-width
                                                          :height game-height})
                                        (play-cljc.transforms/project game-width game-height)
                                        (play-cljc.transforms/color [1 0 0.5 1]))]
                        (when (< i 5)
                          (let [entity (-> entity
                                           (play-cljc.transforms/translate tx ty)
                                           (play-cljc.transforms/rotate r))]
                            (play-cljc.gl.core/render game entity)
                            (recur (inc i) entity))))]}
  (->> (play-cljc.gl.example-utils/init-example card)
       (play-cljc.gl.examples-2d/rotation-multi-example)
       (play-cljc.gl.example-utils/game-loop
         (fn rotation-multi-render [{:keys [entity *state] :as game}]
           (play-cljc.gl.example-utils/resize-example game)
           (play-cljc.gl.core/render game
             {:clear {:color [1 1 1 1] :depth 1}})
           (let [{:keys [tx ty r]} @*state
                 game-width (play-cljc.gl.example-utils/get-width game)
                 game-height (play-cljc.gl.example-utils/get-height game)]
             focus)
           game))))

