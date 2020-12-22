(ns play-cljc.dev
  (:require [play-cljc.gl.examples-2d]
            [play-cljc.gl.examples-3d]
            [play-cljc.gl.examples-advanced]
            [play-cljc.gl.examples-text]
            [play-cljc.macros-js]
            [dynadoc.example :as ex]
            [dynadoc.transform :as transform]
            [clojure.spec.test.alpha :as st]
            [clojure.spec.alpha :as s])
  (:import  [org.lwjgl.glfw GLFW Callbacks GLFWKeyCallbackI]
            [org.lwjgl.opengl GL GL33]))

(defn start [ns-syms]
  (st/instrument)
  (when-not (GLFW/glfwInit)
    (throw (Exception. "Unable to initialize GLFW")))
  (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE GLFW/GLFW_FALSE)
  (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE GLFW/GLFW_TRUE)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MAJOR 3)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MINOR 3)
  (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_FORWARD_COMPAT GL33/GL_TRUE)
  (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_PROFILE GLFW/GLFW_OPENGL_CORE_PROFILE)
  (if-let [window (GLFW/glfwCreateWindow 800 600 "" 0 0)]
    (do
      ;; init
      (GLFW/glfwMakeContextCurrent window)
      (GLFW/glfwSwapInterval 1)
      (GLFW/glfwShowWindow window)
      ;; loop
      (GL/createCapabilities)
      (let [*examples (-> (for [ns-sym ns-syms
                                [example-sym examples] (sort-by first (get @ex/registry-ref ns-sym))
                                example examples]
                            {:ns-sym ns-sym
                             :ex-sym example-sym
                             :ex example
                             :init-form (transform/transform (assoc example :id window))})
                          vec
                          atom)
            *current-example (atom nil)
            init-example (fn [{:keys [ns-sym ex-sym init-form ex] :as example}]
                           (GLFW/glfwSetWindowTitle window
                             (str ns-sym "/" ex-sym "          Press the left and right arrow keys!"))
                           (merge example (eval init-form)))
            switch-to-example! #(swap! *examples update % init-example)]
        (switch-to-example! (reset! *current-example 0))
        (GLFW/glfwSetKeyCallback window
          (reify GLFWKeyCallbackI
            (invoke [this window keycode scancode action mods]
              (when (= action GLFW/GLFW_PRESS)
                (some->
                  (condp = keycode
                    GLFW/GLFW_KEY_LEFT
                    (swap! *current-example (fn [i]
                                              (if (= i 0)
                                                (dec (count @*examples))
                                                (dec i))))
                    GLFW/GLFW_KEY_RIGHT
                    (swap! *current-example (fn [i]
                                              (if (= i (dec (count @*examples)))
                                                0
                                                (inc i))))
                    nil)
                  switch-to-example!)))))
        (while (not (GLFW/glfwWindowShouldClose window))
          (swap! *examples update @*current-example
                 (fn [{:keys [ns-sym ex-sym f game] :as example}]
                   (let [ts (GLFW/glfwGetTime)
                         game (assoc game
                                     :delta-time (- ts (:total-time game))
                                     :total-time ts)]
                     (assoc example :game (f game)))))
          (GLFW/glfwSwapBuffers window)
          (GLFW/glfwPollEvents)))
      ;; clean up
      (Callbacks/glfwFreeCallbacks window)
      (GLFW/glfwDestroyWindow window)
      (GLFW/glfwTerminate))
    (throw (Exception. "Failed to create window"))))

