(ns leiningen.new.play-cljc
  (:require [leiningen.new.templates :as t]
            [clojure.string :as str]))

(defn sanitize-name [s]
  (as-> s $
        (str/trim $)
        (str/lower-case $)
        (str/replace $ "'" "")
        (str/replace $ #"[^a-z0-9]" " ")
        (str/split $ #" ")
        (remove empty? $)
        (str/join "-" $)))

(defn play-cljc-data [name]
  (let [project-name (sanitize-name name)
        core-name "core"]
    (when-not (seq project-name)
      (throw (Exception. (str "Invalid name: " name))))
    {:name project-name
     :core-name core-name
     :project_name (str/replace project-name "-" "_")
     :core_name (str/replace core-name "-" "_")}))

(defn play-cljc*
  [{:keys [project_name core_name] :as data}]
  (let [render (t/renderer "play-cljc")]
    {"README.md" (render "README.md" data)
     ".gitignore" (render "gitignore" data)
     "deps.edn" (render "deps.edn" data)
     "figwheel-main.edn" (render "figwheel-main.edn" data)
     "dev.cljs.edn" (render "dev.cljs.edn" data)
     "dev.clj" (render "dev.clj" data)
     "prod.clj" (render "prod.clj" data)
     "project.clj" (render "project.clj" data)
     (str "src/" project_name "/music.clj") (render "music.clj" data)
     (str "src/" project_name "/" core_name ".cljc") (render "core.cljc" data)
     (str "src/" project_name "/utils.cljc") (render "utils.cljc" data)
     (str "src/" project_name "/start.clj") (render "start.clj" data)
     (str "src/" project_name "/start.cljs") (render "start.cljs" data)
     (str "src/" project_name "/start_dev.clj") (render "start_dev.clj" data)
     (str "src/" project_name "/start_dev.cljs") (render "start_dev.cljs" data)
     "resources/public/index.html" (render "index.html" data)}))

(defn play-cljc
  [name & _]
  (let [data (play-cljc-data name)
        path->content (play-cljc* data)]
    (apply t/->files data (vec path->content))))

