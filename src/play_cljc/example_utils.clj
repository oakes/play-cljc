(ns play-cljc.example-utils
  (:require [play-cljc.core :as c]))

(defn init-example [window]
  (c/create-game window))

(defn resize-example [game])

(defn listen-for-mouse [{:keys [tx ty] :or {tx 0 ty 0}} callback])

(defn get-image [fname callback])

