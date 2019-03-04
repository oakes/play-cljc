(ns play-cljc.macros-js)

(defmacro math [n & args]
  (let [s (str n)]
    (if-let [l (first s)]
      (if (Character/isUpperCase l)
        (symbol (str 'js "/Math." s))
        (cons (symbol (str 'js "/Math." s)) args))
      (throw (Exception. "Invalid method or property name")))))

(defmacro gl [game n & args]
  (let [s (str n)
        remaining-letters (subs s 1)]
    (if-let [l (first s)]
      (if (Character/isUpperCase l)
        `(goog.object/get (:gl ~game) ~s)
        (concat [(symbol (str "." s)) `(:gl ~game)] args))
      (throw (Exception. "Invalid method or property name")))))

