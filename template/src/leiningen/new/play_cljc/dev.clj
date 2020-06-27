(defmulti task first)

(defmethod task :default
  [[task-name]]
  (println "Unknown task:" task-name)
  (System/exit 1))

(require '[figwheel.main :as figwheel])

(defmethod task nil
  [_]
  (figwheel/-main "--build" "dev"))

(require '[{{name}}.start-dev])

(defmethod task "native"
  [_]
  ({{name}}.start-dev/start))

(task *command-line-args*)
