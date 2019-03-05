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
  '[play-cljc.example-utils :as eu]
  '[play-cljc.examples-2d :as e2d])

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
  (if-let [window (GLFW/glfwCreateWindow 300 300 "Hello world!" 0 0)]
    (do
      ;; init
      (GLFW/glfwMakeContextCurrent window)
      (GLFW/glfwSwapInterval 1)
      (GLFW/glfwShowWindow window)
      ;; loop
      (GL/createCapabilities)
      (let [game (eu/init-example window)]
        (e2d/rand-rects-init game)
        (while (not (GLFW/glfwWindowShouldClose window))
          (GLFW/glfwSwapBuffers window)
          (GLFW/glfwPollEvents)))
      ;; clean up
      (Callbacks/glfwFreeCallbacks window)
      (GLFW/glfwDestroyWindow window)
      (GLFW/glfwTerminate))
    (throw (Exception. "Failed to create window"))))

(task *command-line-args*)
