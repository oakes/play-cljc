(ns play-cljc.example-utils
  (:require [play-cljc.core :as c]
            [clojure.java.io :as io])
  (:import [javax.imageio ImageIO]
           [org.lwjgl.glfw GLFW]
           [org.lwjgl.system MemoryUtil]))

(defn init-example [window]
  (c/create-game window))

(defn game-loop [f game state]
  [f game state])

(defn resize-example [game])

(defn listen-for-mouse [{:keys [tx ty] :or {tx 0 ty 0}} callback])

(defn get-image [fname callback]
  (let [image (ImageIO/read (io/input-stream (io/resource (str "dynadoc-extend/cljs/" fname))))]
    (callback {:data image :width (.getWidth image) :height (.getHeight image)})))

(defn get-width [game]
  (let [width (MemoryUtil/memAllocInt 1)
        height (MemoryUtil/memAllocInt 1)
        _ (GLFW/glfwGetFramebufferSize (:context game) width height)
        n (.get width)]
    (MemoryUtil/memFree width)
    (MemoryUtil/memFree height)
    n))

(defn get-height [game]
  (let [width (MemoryUtil/memAllocInt 1)
        height (MemoryUtil/memAllocInt 1)
        _ (GLFW/glfwGetFramebufferSize (:context game) width height)
        n (.get height)]
    (MemoryUtil/memFree width)
    (MemoryUtil/memFree height)
    n))

