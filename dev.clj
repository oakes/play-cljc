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
  (dynadoc/start {:port 5000, :dedupe-pref :cljs})
  (figwheel/-main "--build" "dev"))

(require '[play-cljc.dev :as dev])

(defmethod task "native"
  [_]
  (dev/-main))

(task *command-line-args*)
