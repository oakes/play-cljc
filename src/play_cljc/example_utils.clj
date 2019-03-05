(ns play-cljc.example-utils
  (:require [play-cljc.core :as c]
            [clojure.java.io :as io])
  (:import [javax.imageio ImageIO]))

(defn init-example [window]
  (c/create-game window))

(defn game-loop [f game state]
  [f game state])

(defn resize-example [game])

(defn listen-for-mouse [{:keys [tx ty] :or {tx 0 ty 0}} callback])

(defn get-image [fname callback]
  (let [image (ImageIO/read (io/input-stream fname))]
    (callback {:image image :width (.getWidth image) :height (.getHeight image)})))

