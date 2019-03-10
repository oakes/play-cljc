(ns {{name}}.start
  (:require [{{name}}.{{core-name}} :as c]
            [play-cljc.gl.core :as pc])
  (:import  [org.lwjgl.glfw GLFW Callbacks GLFWCursorPosCallbackI GLFWKeyCallbackI]
            [org.lwjgl.opengl GL GL41]
            [org.lwjgl.system MemoryUtil])
  (:gen-class))

(defn listen-for-mouse [window]
  (GLFW/glfwSetCursorPosCallback window
    (reify GLFWCursorPosCallbackI
      (invoke [this window xpos ypos]
        (swap! c/*state
          (fn [state]
            (let [*fb-width (MemoryUtil/memAllocInt 1)
                  *fb-height (MemoryUtil/memAllocInt 1)
                  *window-width (MemoryUtil/memAllocInt 1)
                  *window-height (MemoryUtil/memAllocInt 1)
                  _ (GLFW/glfwGetFramebufferSize window *fb-width *fb-height)
                  _ (GLFW/glfwGetWindowSize window *window-width *window-height)
                  fb-width (.get *fb-width)
                  fb-height (.get *fb-height)
                  window-width (.get *window-width)
                  window-height (.get *window-height)
                  width-ratio (/ fb-width window-width)
                  height-ratio (/ fb-height window-height)
                  x (- (* xpos width-ratio))
                  y (- (* ypos height-ratio))]
              (MemoryUtil/memFree *fb-width)
              (MemoryUtil/memFree *fb-height)
              (MemoryUtil/memFree *window-width)
              (MemoryUtil/memFree *window-height)
              (assoc state :mouse-x x :mouse-y y))))))))

(defn keycode->keyword [keycode]
  (condp = keycode
    GLFW/GLFW_KEY_LEFT :left
    GLFW/GLFW_KEY_RIGHT :right
    GLFW/GLFW_KEY_UP :up
    nil))

(defn listen-for-keys [window]
  (GLFW/glfwSetKeyCallback window
    (reify GLFWKeyCallbackI
      (invoke [this window keycode scancode action mods]
        (when-let [k (keycode->keyword keycode)]
          (condp = action
            GLFW/GLFW_PRESS (swap! c/*state update :pressed-keys conj k)
            GLFW/GLFW_RELEASE (swap! c/*state update :pressed-keys disj k)
            nil))))))

(defn -main [& args]
  (when-not (GLFW/glfwInit)
    (throw (Exception. "Unable to initialize GLFW")))
  (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE GLFW/GLFW_FALSE)
  (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE GLFW/GLFW_TRUE)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MAJOR 4)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MINOR 1)
  (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_FORWARD_COMPAT GL41/GL_TRUE)
  (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_PROFILE GLFW/GLFW_OPENGL_CORE_PROFILE)
  (if-let [window (GLFW/glfwCreateWindow 800 600 "Hello, world!" 0 0)]
    (do
      (GLFW/glfwMakeContextCurrent window)
      (GLFW/glfwSwapInterval 1)
      (GLFW/glfwShowWindow window)
      (GL/createCapabilities)
      (listen-for-mouse window)
      (listen-for-keys window)
      (let [initial-game (assoc (pc/->game window)
                                :delta-time 0
                                :total-time 0)]
        (c/init initial-game)
        (loop [game initial-game]
          (when-not (GLFW/glfwWindowShouldClose window)
            (let [ts (GLFW/glfwGetTime)
                  game (assoc game
                              :delta-time (- ts (:total-time game))
                              :total-time ts)
                  game (c/run game)]
              (GLFW/glfwSwapBuffers window)
              (GLFW/glfwPollEvents)
              (recur game)))))
      (Callbacks/glfwFreeCallbacks window)
      (GLFW/glfwDestroyWindow window)
      (GLFW/glfwTerminate))
    (throw (Exception. "Failed to create window"))))

