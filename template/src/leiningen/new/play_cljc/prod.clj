(defmulti task first)

(defmethod task :default
  [[task-name]]
  (println "Unknown task:" task-name)
  (System/exit 1))

(require
  '[cljs.build.api :as api]
  '[clojure.java.io :as io]
  '[clojure.string :as str]
  '[leiningen.core.project :as p :refer [defproject]]
  '[leiningen.clean :refer [clean]]
  '[leiningen.uberjar :refer [uberjar]])

(defn read-project-clj []
  (p/ensure-dynamic-classloader)
  (-> "project.clj" load-file var-get))

(defn parse-coord [coord]
  (let [[artifact info] coord
        s (str artifact)
        i (str/index-of s "$")]
    (if i
      [(symbol (subs s 0 i))
       (assoc info :classifier (subs s (inc i)))]
      coord)))

(defn read-deps-edn [aliases-to-include]
  (let [{:keys [paths deps aliases]} (-> "deps.edn" slurp clojure.edn/read-string)
        deps (->> (select-keys aliases aliases-to-include)
                  vals
                  (mapcat :extra-deps)
                  (into deps)
                  (map parse-coord)
                  (reduce
                    (fn [deps [artifact info]]
                      (if-let [version (:mvn/version info)]
                        (conj deps
                          (transduce cat conj [artifact version]
                            (select-keys info [:exclusions :classifier])))
                        deps))
                    []))
        paths (->> (select-keys aliases aliases-to-include)
                   vals
                   (mapcat :extra-paths)
                   (into paths))]
    {:dependencies deps
     :source-paths []
     :resource-paths paths}))

(defn delete-children-recursively! [f]
  (when (.isDirectory f)
    (doseq [f2 (.listFiles f)]
      (delete-children-recursively! f2)))
  (when (.exists f) (io/delete-file f)))

(defmethod task nil
  [_]
  (let [out-file "resources/public/main.js"
        out-dir "resources/public/main.out"]
    (println "Building main.js")
    (delete-children-recursively! (io/file out-dir))
    (api/build "src" {:main          '{{name}}.start
                      :optimizations :advanced
                      :output-to     out-file
                      :output-dir    out-dir})
    (delete-children-recursively! (io/file out-dir))
    (println "Build complete:" out-file)
    (System/exit 0)))

(defmethod task "uberjar"
  [_]
  (let [project (-> (read-project-clj)
                    (merge (read-deps-edn [:linux :macos :windows]))
                    p/init-project)]
    (clean project)
    (uberjar project)
    (System/exit 0)))

(task *command-line-args*)
