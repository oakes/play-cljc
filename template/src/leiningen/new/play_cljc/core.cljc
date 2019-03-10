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
               (e/->image-entity game data width height))))))

(defn run [game]
  game)

