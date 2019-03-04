(ns play-cljc.math-js)

(defmacro math [n & args]
  (let [s (str n)]
    (if-let [l (first s)]
      (if (Character/isUpperCase l)
        (symbol (str 'js "/Math." s))
        (cons (symbol (str 'js "/Math." s)) args))
      (throw (Exception. "Invalid method or property name")))))

