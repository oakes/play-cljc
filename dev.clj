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
  '[iglu.core :as ig]
  '[play-cljc.utils :as u]
  '[play-cljc.example-data :as data])

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
      (let [program (u/create-program nil
                      (ig/iglu->glsl :vertex data/two-d-vertex-shader)
                      (ig/iglu->glsl :fragmen data/two-d-fragment-shader))]
        (GL41/glClearColor (float 1) (float 1) (float 1) (float 1))
        (while (not (GLFW/glfwWindowShouldClose window))
          (GL41/glClear (bit-or GL41/GL_COLOR_BUFFER_BIT GL41/GL_DEPTH_BUFFER_BIT))
          (GLFW/glfwSwapBuffers window)
          (GLFW/glfwPollEvents)))
      ;; clean up
      (Callbacks/glfwFreeCallbacks window)
      (GLFW/glfwDestroyWindow window)
      (GLFW/glfwTerminate))
    (throw (Exception. "Failed to create window"))))

(task *command-line-args*)
