(defmulti task first)

(defmethod task :default
  [_]
  (let [all-tasks  (-> task methods (dissoc :default) keys sort)
        interposed (->> all-tasks (interpose ", ") (apply str))]
    (println "Unknown or missing task. Choose one of:" interposed)
    (System/exit 1)))

(require
  '[figwheel.main :as figwheel]
  '[dynadoc.core :as dynadoc])

(defmethod task nil
  [_]
  (dynadoc/-main "--port" "5000")
  (figwheel/-main "--build" "dev"))

(import '[org.lwjgl.glfw GLFW Callbacks]
        '[org.lwjgl.opengl GL GL41])

(require
  '[play-cljc.examples-2d]
  '[play-cljc.examples-3d]
  '[play-cljc.examples-advanced]
  '[dynadoc.example :as ex])

(defmethod task "native"
  [_]
  (when-not (GLFW/glfwInit)
    (throw (Exception. "Unable to initialize GLFW")))
  (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE GLFW/GLFW_FALSE)
  (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE GLFW/GLFW_TRUE)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MAJOR 4)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MINOR 1)
  (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_FORWARD_COMPAT GL41/GL_TRUE)
  (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_PROFILE GLFW/GLFW_OPENGL_CORE_PROFILE)
  (if-let [window (GLFW/glfwCreateWindow 800 600 "Hello world!" 0 0)]
    (do
      ;; init
      (GLFW/glfwMakeContextCurrent window)
      (GLFW/glfwSwapInterval 1)
      (GLFW/glfwShowWindow window)
      ;; loop
      (GL/createCapabilities)
      (let [example (-> @ex/registry-ref
                        (get-in ['play-cljc.examples-3d 'translation-3d])
                        first
                        (or (throw (Exception. "Example not found"))))
            [f game state] (eval
                             (list 'let [(:with-card example) window]
                               (:body example)))]
        (loop [state state]
          (when-not (GLFW/glfwWindowShouldClose window)
            (let [new-state (f (assoc game :time (GLFW/glfwGetTime)) state)]
              (GLFW/glfwSwapBuffers window)
              (GLFW/glfwPollEvents)
              (recur new-state)))))
      ;; clean up
      (Callbacks/glfwFreeCallbacks window)
      (GLFW/glfwDestroyWindow window)
      (GLFW/glfwTerminate))
    (throw (Exception. "Failed to create window"))))

(task *command-line-args*)
