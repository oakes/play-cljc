(ns play-cljc.macros-java
  (:require [play-cljc.transforms :as t]))

(defmacro math
  "Wraps java.lang.Math, calling a method if the provided symbol starts with a
  lower-case letter, or a static field if it starts with an upper-case letter."
  [n & args]
  (let [s (str n)
        ^Character l (nth s 0)]
    (if (Character/isUpperCase l)
      (symbol (str 'Math "/" s))
      (cons (symbol (str 'Math "/" s)) args))))

(defmacro gl
  "Wraps org.lwjgl.opengl.GL33, calling a method if the provided symbol starts with a
  lower-case letter, or a static field if it starts with an upper-case letter."
  [_ n & args]
  (let [s (str n)
        ^Character l (nth s 0)
        remaining-letters (subs s 1)]
    (if (Character/isUpperCase l)
      (symbol (str "org.lwjgl.opengl.GL33/GL_" s))
      (cons (symbol (str "org.lwjgl.opengl.GL33/gl" (Character/toUpperCase l) remaining-letters))
            args))))

(defmacro transform
  "Work in progress! This macro is subject to change/break in future releases."
  ([content]
   (t/transform content))
  ([entity transforms]
   (t/transform-entity entity transforms)))

