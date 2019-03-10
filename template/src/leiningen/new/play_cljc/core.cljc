(ns {{name}}.{{core-name}}
  (:require [{{name}}.utils :as utils]
            [play-cljc.gl.core :as c]
            [play-cljc.gl.entities-2d :as e]
            #?(:clj  [play-cljc.macros-java :refer [gl math transform]]
               :cljs [play-cljc.macros-js :refer-macros [gl math transform]])))

(defonce *state (atom {:mouse-x 0
                       :mouse-y 0
                       :entities []}))

(defn init [game]
  (doseq [path ["player_walk1.png" "player_walk2.png" "player_walk3.png"]]
    (utils/get-image path
      (fn [{:keys [data width height]}]
        (swap! *state update :entities conj
               (assoc (e/->image-entity game data width height)
                      :width width :height height))))))

(def screen-entity
  {:viewport {:x 0 :y 0 :width 0 :height 0}
   :clear {:color [(/ 173 255) (/ 216 255) (/ 230 255) 1] :depth 1}})

(defn run [game]
  (let [{:keys [entities]} @*state
        player (first entities)
        game-width (utils/get-width game)
        game-height (utils/get-height game)
        player-width (/ game-width 10)
        player-height (* player-width (/ (:height player) (:width player)))]
    (c/render game (update screen-entity :viewport
                           assoc :width game-width :height game-height))
    (doseq [entity (transform
                     [{:project {:width game-width :height game-height}
                       :translate {:x 0 :y 0}
                       :scale {:x player-width :y player-height}}
                      player])]
      (c/render game entity)))
  game)

