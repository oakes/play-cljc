(defproject {{name}} "0.1.0-SNAPSHOT"
  :repositories [["clojars" {:url "https://clojars.org/repo"
                             :sign-releases false}]]
  :clean-targets ^{:protect false} ["target"]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [play-cljc "0.9.6"]
                 [org.lwjgl/lwjgl "3.3.1"]
                 [org.lwjgl/lwjgl-glfw "3.3.1"]
                 [org.lwjgl/lwjgl-opengl "3.3.1"]
                 [org.lwjgl/lwjgl-stb "3.3.1"]
                 [org.lwjgl/lwjgl "3.3.1" :classifier "natives-linux"]
                 [org.lwjgl/lwjgl-glfw "3.3.1" :classifier "natives-linux"]
                 [org.lwjgl/lwjgl-opengl "3.3.1" :classifier "natives-linux"]
                 [org.lwjgl/lwjgl-stb "3.3.1" :classifier "natives-linux"]
                 [org.lwjgl/lwjgl "3.3.1" :classifier "natives-macos"]
                 [org.lwjgl/lwjgl-glfw "3.3.1" :classifier "natives-macos"]
                 [org.lwjgl/lwjgl-opengl "3.3.1" :classifier "natives-macos"]
                 [org.lwjgl/lwjgl-stb "3.3.1" :classifier "natives-macos"]
                 [org.lwjgl/lwjgl "3.3.1" :classifier "natives-macos-arm64"]
                 [org.lwjgl/lwjgl-glfw "3.3.1" :classifier "natives-macos-arm64"]
                 [org.lwjgl/lwjgl-opengl "3.3.1" :classifier "natives-macos-arm64"]
                 [org.lwjgl/lwjgl-stb "3.3.1" :classifier "natives-macos-arm64"]
                 [org.lwjgl/lwjgl "3.3.1" :classifier "natives-windows"]
                 [org.lwjgl/lwjgl-glfw "3.3.1" :classifier "natives-windows"]
                 [org.lwjgl/lwjgl-opengl "3.3.1" :classifier "natives-windows"]
                 [org.lwjgl/lwjgl-stb "3.3.1" :classifier "natives-windows"]]
  :jvm-opts ~(if (= "Mac OS X" (System/getProperty "os.name"))
               ["-XstartOnFirstThread"]
               [])
  :main {{name}}.start
  :aot [{{name}}.start])
