(ns play-cljc.math-js)

(defmacro math-call [method & args]
  (cons (symbol (str 'js "/Math." method)) args))

(defmacro math-prop [prop]
  (symbol (str 'js "/Math." prop)))

