(defproject {{name}} "0.1.0-SNAPSHOT"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :repositories [["clojars" {:url "https://clojars.org/repo"
                             :sign-releases false}]]
  :clean-targets ^{:protect false} ["target"]
  :main {{name}}.start
  :aot [{{name}}.start])
