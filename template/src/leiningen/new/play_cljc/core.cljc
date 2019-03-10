(ns {{name}}.{{core-name}}
  (:require [{{name}}.utils :as utils]
            [{{name}}.move :as move]
            [play-cljc.gl.core :as c]
            [play-cljc.gl.entities-2d :as e]
            #?(:clj  [play-cljc.macros-java :refer [gl math transform]]
               :cljs [play-cljc.macros-js :refer-macros [gl math transform]])))

(defonce *state (atom {:mouse-x 0
                       :mouse-y 0
                       :pressed-keys #{}
                       :x-velocity 0
                       :y-velocity 0
                       :player-x 0
                       :player-y 0
                       :can-jump? false
                       :direction :right
                       :entities {}}))

(defn init [game]
  ;; allow transparency in images
  (gl game enable (gl game BLEND))
  (gl game blendFunc (gl game SRC_ALPHA) (gl game ONE_MINUS_SRC_ALPHA))
  ;; load images and put them in the state atom
  (doseq [[k path] {:walk1 "player_walk1.png"
                    :walk2 "player_walk2.png"
                    :walk3 "player_walk3.png"}]
    (utils/get-image path
      (fn [{:keys [data width height]}]
        (swap! *state update :entities assoc k
               (assoc (e/->image-entity game data width height)
                      :width width :height height))))))

(def screen-entity
  {:viewport {:x 0 :y 0 :width 0 :height 0}
   :clear {:color [(/ 173 255) (/ 216 255) (/ 230 255) 1] :depth 1}})

(defn run [game]
  (let [{:keys [entities
                pressed-keys
                player-x
                player-y]
         :as state} @*state
        player (:walk1 entities)
        game-width (utils/get-width game)
        game-height (utils/get-height game)
        player-width (/ game-width 10)
        player-height (* player-width (/ (:height player) (:width player)))]
    ;; render the blue background
    (c/render game (update screen-entity :viewport
                           assoc :width game-width :height game-height))
    ;; render the player
    (doseq [entity (transform
                     [{:project {:width game-width :height game-height}}
                      [{:translate {:x player-x :y player-y}
                        :scale {:x player-width :y player-height}}
                       player]])]
      (c/render game entity))
    ;; change the state to move the player
    (swap! *state
      (fn [state]
        (->> (assoc state
                    :player-width player-width
                    :player-height player-height)
             (move/move game)
             (move/prevent-move game)))))
  ;; return the game map
  game)

