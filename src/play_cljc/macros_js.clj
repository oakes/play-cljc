(ns play-cljc.macros-js
  (:require [play-cljc.transforms :as t]))

(defmacro math [n & args]
  (let [s (str n)
        l (nth s 0)]
    (if (Character/isUpperCase l)
      (symbol (str 'js "/Math." s))
      (cons (symbol (str 'js "/Math." s)) args))))

(defmacro gl [game n & args]
  (let [s (str n)
        l (nth s 0)]
    (if (Character/isUpperCase l)
      `(goog.object/get (:context ~game) ~s)
      (concat [(symbol (str "." s)) `(:context ~game)] args))))

(defmacro transform [content]
  (t/transform content))

