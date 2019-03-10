(ns play-cljc.macros-java
  (:require [play-cljc.transforms :as t]))

(defmacro math [n & args]
  (let [s (str n)
        l (nth s 0)]
    (if (Character/isUpperCase l)
      (symbol (str 'Math "/" s))
      (cons (symbol (str 'Math "/" s)) args))))

(defmacro gl [_ n & args]
  (let [s (str n)
        l (nth s 0)
        remaining-letters (subs s 1)]
    (if (Character/isUpperCase l)
      (symbol (str "org.lwjgl.opengl.GL41/GL_" s))
      (cons (symbol (str "org.lwjgl.opengl.GL41/gl" (Character/toUpperCase l) remaining-letters))
            args))))

(defmacro transform [content]
  (t/transform content))

