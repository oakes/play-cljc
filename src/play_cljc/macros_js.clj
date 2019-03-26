(ns play-cljc.macros-js
  (:require [play-cljc.transforms :as t]))

(defmacro math
  "Wraps the Math object, calling a method if the provided symbol starts with a
  lower-case letter, or a property if it starts with an upper-case letter."
  [n & args]
  (let [s (str n)
        ^Character l (nth s 0)]
    (if (Character/isUpperCase l)
      (symbol (str 'js "/Math." s))
      (cons (symbol (str 'js "/Math." s)) args))))

(defmacro gl
  "Wraps the WebGL2RenderingContext object, calling a method if the provided symbol starts with a
  lower-case letter, or a static field if it starts with an upper-case letter."
  [game n & args]
  (let [s (str n)
        ^Character l (nth s 0)]
    (if (Character/isUpperCase l)
      `(goog.object/get (:context ~game) ~s)
      (concat [(symbol (str "." s)) `(:context ~game)] args))))

(defmacro transform
  "Work in progress! This macro is subject to change/break in future releases."
  ([content]
   (t/transform content))
  ([attrs entity]
   (t/transform-entity attrs entity)))

