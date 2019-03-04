(ns play-cljc.math-java)

(defmacro math [n & args]
  (let [s (str n)]
    (if-let [l (first s)]
      (if (Character/isUpperCase l)
        (symbol (str 'Math "/" s))
        (cons (symbol (str 'Math "/" s)) args))
      (throw (Exception. "Invalid method or property name")))))

