(ns {{name}}.{{core-name}}
  (:require [{{name}}.utils :as utils]
            [play-cljc.gl.core :as c]
            [play-cljc.gl.entities-2d :as e]
            #?(:clj  [play-cljc.macros-java :refer [gl math transform]]
               :cljs [play-cljc.macros-js :refer-macros [gl math transform]])))

(defonce *state (atom {:mouse-x 0
                       :mouse-y 0}))

(defn run [game]
  game)

