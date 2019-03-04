(ns play-cljc.macros-js)

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
      `(goog.object/get (:gl ~game) ~s)
      (concat [(symbol (str "." s)) `(:gl ~game)] args))))

