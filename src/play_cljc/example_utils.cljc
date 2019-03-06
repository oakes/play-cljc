(ns play-cljc.example-utils
  (:require [play-cljc.core :as c]
            [play-cljc.math :as m]
            #?(:clj  [clojure.java.io :as io]
               :cljs [goog.events :as events]))
  #?(:clj (:import [java.nio ByteBuffer]
                   [org.lwjgl.glfw GLFW GLFWCursorPosCallback]
                   [org.lwjgl.system MemoryUtil]
                   [org.lwjgl.stb STBImage])))

(defn init-example [window]
  #?(:clj  (c/create-game window)
     :cljs (do
             (when-let [canvas (.querySelector window "canvas")]
               (.removeChild window canvas))
             (let [canvas (doto (js/document.createElement "canvas")
                            (-> .-style .-width (set! "100%"))
                            (-> .-style .-height (set! "100%")))
                   context (.getContext canvas "webgl2")]
               (.appendChild window canvas)
               (c/create-game context)))))

(defn game-loop [f game state]
  #?(:clj  [f game state]
     :cljs (let [new-state (f game state)]
             (js/requestAnimationFrame #(game-loop f game new-state)))))

(defn resize-example [{:keys [context] :as game}]
  #?(:cljs (let [display-width context.canvas.clientWidth
                 display-height context.canvas.clientHeight]
             (when (or (not= context.canvas.width display-width)
                       (not= context.canvas.height display-height))
               (set! context.canvas.width display-width)
               (set! context.canvas.height display-height)))))

(defn listen-for-mouse [{:keys [context] :as game} *state]
  #?(:clj  (GLFW/glfwSetCursorPosCallback context
             (proxy [GLFWCursorPosCallback] []
               (invoke [window xpos ypos]
                 (swap! *state
                   (fn [{:keys [tx ty] :or {tx 0 ty 0} :as state}]
                     (let [fb-width (MemoryUtil/memAllocInt 1)
                           fb-height (MemoryUtil/memAllocInt 1)
                           window-width (MemoryUtil/memAllocInt 1)
                           window-height (MemoryUtil/memAllocInt 1)
                           _ (GLFW/glfwGetFramebufferSize context fb-width fb-height)
                           _ (GLFW/glfwGetWindowSize context window-width window-height)
                           width-ratio (/ (.get fb-width) (.get window-width))
                           height-ratio (/ (.get fb-height) (.get window-height))]
                       (MemoryUtil/memFree fb-width)
                       (MemoryUtil/memFree fb-height)
                       (MemoryUtil/memFree window-width)
                       (MemoryUtil/memFree window-height)
                       (assoc state :x (* xpos width-ratio) :y (* ypos height-ratio))))))))
     :cljs (events/listen js/window "mousemove"
             (fn [event]
               (swap! *state
                 (fn [{:keys [tx ty] :or {tx 0 ty 0} :as state}]
                   (let [bounds (.getBoundingClientRect context.canvas)
                         x (- (.-clientX event) (.-left bounds) tx)
                         y (- (.-clientY event) (.-top bounds) ty)
                         rx (/ x (.-width bounds))
                         ry (/ y (.-height bounds))
                         r (Math/atan2 rx ry)
                         cx (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
                         cy (- (.-height bounds)
                               (- (.-clientY event) (.-top bounds)))
                         cr (-> (/ cx (.-width bounds))
                                (* 360)
                                m/deg->rad)]
                     (assoc state :x x :y y :rx rx :ry ry :r r :cx cx :cy cy :cr cr))))))))

(defn get-image [fname callback]
  #?(:clj  (let [is (io/input-stream (io/resource (str "dynadoc-extend/cljs/" fname)))
                 bytes (with-open [out (java.io.ByteArrayOutputStream.)]
                         (io/copy is out)
                         (.toByteArray out))
                 width (MemoryUtil/memAllocInt 1)
                 height (MemoryUtil/memAllocInt 1)
                 components (MemoryUtil/memAllocInt 1)
                 direct-buffer (doto (ByteBuffer/allocateDirect (alength bytes))
                                 (.put bytes)
                                 (.flip))
                 decoded-image (STBImage/stbi_load_from_memory
                                 direct-buffer width height components
                                 STBImage/STBI_rgb_alpha)
                 image {:data decoded-image
                        :width (.get width)
                        :height (.get height)}]
             (MemoryUtil/memFree width)
             (MemoryUtil/memFree height)
             (MemoryUtil/memFree components)
             (callback image))
     :cljs (let [image (js/Image.)]
             (doto image
               (-> .-src (set! fname))
               (-> .-onload (set! #(callback {:data image
                                              :width image.width
                                              :height image.height})))))))

(defn get-width [game]
  #?(:clj  (let [width (MemoryUtil/memAllocInt 1)
                 height (MemoryUtil/memAllocInt 1)
                 _ (GLFW/glfwGetFramebufferSize (:context game) width height)
                 n (.get width)]
             (MemoryUtil/memFree width)
             (MemoryUtil/memFree height)
             n)
     :cljs (-> game :context .-canvas .-clientWidth)))

(defn get-height [game]
  #?(:clj  (let [width (MemoryUtil/memAllocInt 1)
                 height (MemoryUtil/memAllocInt 1)
                 _ (GLFW/glfwGetFramebufferSize (:context game) width height)
                 n (.get height)]
             (MemoryUtil/memFree width)
             (MemoryUtil/memFree height)
             n)
     :cljs (-> game :context .-canvas .-clientHeight)))

