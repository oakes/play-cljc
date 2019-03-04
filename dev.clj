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
  '[play-cljc.utils :as u]
  '[play-cljc.example-data :as data]
  '[play-cljc.math :as m]
  '[play-cljc.core :as c]
  '[play-cljc.example-utils :as eu]
  '[play-cljc.macros-java :refer [gl]])

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
      (let [game (eu/init-example window)
            entity (c/create-entity game
                     {:vertex data/two-d-vertex-shader
                      :fragment data/two-d-fragment-shader
                      :attributes {'a_position {:data data/rect
                                                :type (gl game FLOAT)
                                                :size 2}}})
            color [(rand) (rand) (rand) 1]]
        (while (not (GLFW/glfwWindowShouldClose window))
          (c/render-entity game
            (assoc entity
              :clear {:color [1 1 1 1] :depth 1}
              :viewport {:x 0 :y 0 :width (u/get-width game) :height (u/get-height game)}
              :uniforms {'u_color color
                         'u_matrix (->> (m/projection-matrix 300 300)
                                        (m/multiply-matrices 3 (m/translation-matrix 0 0))
                                        (m/multiply-matrices 3 (m/scaling-matrix 100 100))
                                        float-array)}))
          (GLFW/glfwSwapBuffers window)
          (GLFW/glfwPollEvents)))
      ;; clean up
      (Callbacks/glfwFreeCallbacks window)
      (GLFW/glfwDestroyWindow window)
      (GLFW/glfwTerminate))
    (throw (Exception. "Failed to create window"))))

(task *command-line-args*)
