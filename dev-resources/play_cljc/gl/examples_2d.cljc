(ns play-cljc.gl.examples-2d
  "2D examples based on content from webgl2fundamentals.org"
  (:require [play-cljc.gl.core :as c]
            [play-cljc.gl.entities-2d :as e]
            [play-cljc.gl.example-utils :as eu]
            [play-cljc.gl.example-data :as data]
            [play-cljc.transforms :as t]
            [play-cljc.primitives-2d :as primitives]
            #?(:clj  [play-cljc.macros-java :refer [gl]]
               :cljs [play-cljc.macros-js :refer-macros [gl]])
            #?(:clj [dynadoc.example :refer [defexample]]))
  #?(:cljs (:require-macros [dynadoc.example :refer [defexample]])))

;; rand-rects

(defn rand-rects-render [game [entity rects :as state]]
  (eu/resize-example game)
  (c/render game
    {:clear {:color [1 1 1 1] :depth 1}})
  (doseq [{color :color
           [posx posy] :position
           [sx sy] :scale}
          rects]
    (c/render game
      (-> entity
          (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/color color)
          (t/project {:width (eu/get-width game) :height (eu/get-height game)})
          (t/translate {:x posx :y posy})
          (t/scale {:x sx :y sy}))))
  state)

(defn rand-rects-init [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  [(e/->entity game primitives/rect)
   (for [_ (range 50)]
     {:color [(rand) (rand) (rand) 1]
      :position [(rand-int (eu/get-width game)) (rand-int (eu/get-height game))]
      :scale [(rand-int 300) (rand-int 300)]})])

(defexample play-cljc.gl.examples-2d/rand-rects
  {:with-card card}
  (let [game (play-cljc.gl.example-utils/init-example card)
        state (play-cljc.gl.examples-2d/rand-rects-init game)]
    (play-cljc.gl.example-utils/game-loop
      play-cljc.gl.examples-2d/rand-rects-render
      game state)))

;; image

(defn image-render [game [entity {:keys [width height]} :as state]]
  (eu/resize-example game)
  (let [game-width (eu/get-width game)
        game-height (eu/get-height game)
        screen-ratio (/ game-width game-height)
        image-ratio (/ width height)
        img-scale (if (> screen-ratio image-ratio)
                    {:x (* game-height (/ width height)) :y game-height}
                    {:x game-width :y (* game-width (/ height width))})]
    (c/render game
      (-> entity
          (assoc :viewport {:x 0 :y 0 :width game-width :height game-height})
          (t/project {:width game-width :height game-height})
          (t/translate {:x 0 :y 0})
          (t/scale img-scale))))
  state)

(defn image-init [game {:keys [data width height] :as image}]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  [(assoc (e/->image-entity game data width height)
     :clear {:color [1 1 1 1] :depth 1})
   image])

(defexample play-cljc.gl.examples-2d/image
  {:with-card card}
  (let [game (play-cljc.gl.example-utils/init-example card)]
    (play-cljc.gl.example-utils/get-image "aintgottaexplainshit.jpg"
      (fn [image]
        (let [state (play-cljc.gl.examples-2d/image-init game image)]
          (play-cljc.gl.example-utils/game-loop
            play-cljc.gl.examples-2d/image-render
            game state))))))

;; translation

(defn translation-render [game [entity *state :as state]]
  (eu/resize-example game)
  (let [{:keys [x y]} @*state]
    (c/render game
      (-> entity
          (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project {:width (eu/get-width game) :height (eu/get-height game)})
          (t/translate {:x x :y y}))))
  state)

(defn translation-init [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (-> (e/->entity game data/f-2d)
                   (t/color [1 0 0.5 1])
                   (assoc :clear {:color [0 0 0 0] :depth 1}))
        *state (atom {:x 0 :y 0})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.gl.examples-2d/translation
  {:with-card card}
  (let [game (play-cljc.gl.example-utils/init-example card)
        state (play-cljc.gl.examples-2d/translation-init game)]
    (play-cljc.gl.example-utils/game-loop
      play-cljc.gl.examples-2d/translation-render
      game state)))

;; rotation

(defn rotation-render [game [entity *state :as state]]
  (eu/resize-example game)
  (let [{:keys [tx ty r]} @*state]
    (c/render game
      (-> entity
          (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project {:width (eu/get-width game) :height (eu/get-height game)})
          (t/translate {:x tx :y ty})
          (t/rotate {:angle r})
          ;; make it rotate around its center
          (t/translate {:x -50 :y -75}))))
  state)

(defn rotation-init [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (-> (e/->entity game data/f-2d)
                   (t/color [1 0 0.5 1])
                   (assoc :clear {:color [0 0 0 0] :depth 1}))
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.gl.examples-2d/rotation
  {:with-card card}
  (let [game (play-cljc.gl.example-utils/init-example card)
        state (play-cljc.gl.examples-2d/rotation-init game)]
    (play-cljc.gl.example-utils/game-loop
      play-cljc.gl.examples-2d/rotation-render
      game state)))

;; scale

(defn scale-render [game [entity *state :as state]]
  (eu/resize-example game)
  (let [{:keys [tx ty rx ry]} @*state]
    (c/render game
      (-> entity
          (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
          (t/project {:width (eu/get-width game) :height (eu/get-height game)})
          (t/translate {:x tx :y ty})
          (t/rotate {:angle 0})
          (t/scale {:x rx :y ry}))))
  state)

(defn scale-init [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (-> (e/->entity game data/f-2d)
                   (t/color [1 0 0.5 1])
                   (assoc :clear {:color [0 0 0 0] :depth 1}))
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :rx 1 :ry 1})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.gl.examples-2d/scale
  {:with-card card}
  (let [game (play-cljc.gl.example-utils/init-example card)
        state (play-cljc.gl.examples-2d/scale-init game)]
    (play-cljc.gl.example-utils/game-loop
      play-cljc.gl.examples-2d/scale-render
      game state)))

;; rotation-multi

(defn rotation-multi-render [game [entity *state :as state]]
  (eu/resize-example game)
  (c/render game
    {:clear {:color [1 1 1 1] :depth 1}})
  (let [{:keys [tx ty r]} @*state]
    (loop [i 0
           entity (-> entity
                      (assoc :viewport {:x 0 :y 0 :width (eu/get-width game) :height (eu/get-height game)})
                      (t/project {:width (eu/get-width game) :height (eu/get-height game)}))]
      (when (< i 5)
        (let [entity (-> entity
                         (t/translate {:x tx :y ty})
                         (t/rotate {:angle r}))]
          (c/render game entity)
          (recur (inc i) entity)))))
  state)

(defn rotation-multi-init [game]
  (gl game disable (gl game CULL_FACE))
  (gl game disable (gl game DEPTH_TEST))
  (let [entity (-> (e/->entity game data/f-2d)
                   (t/color [1 0 0.5 1]))
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (eu/listen-for-mouse game *state)
    [entity *state]))

(defexample play-cljc.gl.examples-2d/rotation-multi
  {:with-card card}
  (let [game (play-cljc.gl.example-utils/init-example card)
        state (play-cljc.gl.examples-2d/rotation-multi-init game)]
    (play-cljc.gl.example-utils/game-loop
      play-cljc.gl.examples-2d/rotation-multi-render
      game state)))

