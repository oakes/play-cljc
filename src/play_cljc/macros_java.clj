(ns play-cljc.macros-java)

(defmacro math [n & args]
  (let [s (str n)]
    (if-let [l (first s)]
      (if (Character/isUpperCase l)
        (symbol (str 'Math "/" s))
        (cons (symbol (str 'Math "/" s)) args))
      (throw (Exception. "Invalid method or property name")))))

(defmacro gl [_ n & args]
  (let [s (str n)
        remaining-letters (subs s 1)]
    (if-let [l (first s)]
      (if (Character/isUpperCase l)
        (symbol (str "org.lwjgl.opengl.GL41/GL_" s))
        (cons (symbol (str "org.lwjgl.opengl.GL41/gl" (Character/toUpperCase l) remaining-letters))
              args))
      (throw (Exception. "Invalid method or property name")))))

