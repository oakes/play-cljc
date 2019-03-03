(ns play-cljc.math-java)

(defmacro math [method & args]
  (cons (symbol (str 'Math "/" method)) args))

(defmacro math-prop [prop]
  (symbol (str 'Math "/" prop)))

