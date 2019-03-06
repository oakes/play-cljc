(ns play-cljc.example-utils
  (:require [play-cljc.core :as c]
            [clojure.java.io :as io])
  (:import [java.nio ByteBuffer]
           [org.lwjgl.glfw GLFW]
           [org.lwjgl.system MemoryUtil]
           [org.lwjgl.stb STBImage]))

(defn init-example [window]
  (c/create-game window))

(defn game-loop [f game state]
  [f game state])

(defn resize-example [game])

(defn listen-for-mouse [{:keys [tx ty] :or {tx 0 ty 0}} callback])

(defn get-image [fname callback]
  (let [is (io/input-stream (io/resource (str "dynadoc-extend/cljs/" fname)))
        bytes (with-open [out (java.io.ByteArrayOutputStream.)]
                (io/copy is out)
                (.toByteArray out))
        width (MemoryUtil/memAllocInt 1)
        height (MemoryUtil/memAllocInt 1)
        components (MemoryUtil/memAllocInt 1)
        direct-buffer (doto (ByteBuffer/allocateDirect (alength bytes))
                        (.put bytes)
                        (.flip))
        decoded-image (STBImage/stbi_load_from_memory direct-buffer width height components 4)
        image {:data decoded-image :width (.get width) :height (.get height)}]
    (MemoryUtil/memFree width)
    (MemoryUtil/memFree height)
    (MemoryUtil/memFree components)
    (callback image)))

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

