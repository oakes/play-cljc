(ns play-cljc.math-js)

(defmacro math [method & args]
  (cons (symbol (str 'js "/Math." method)) args))

(defmacro math-prop [prop]
  (symbol (str 'js "/Math." prop)))

