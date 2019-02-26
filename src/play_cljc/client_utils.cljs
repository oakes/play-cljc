(ns play-cljc.client-utils)

(defn create-canvas [card]
  (when-let [canvas (.querySelector card "canvas")]
    (.removeChild card canvas))
  (let [canvas (doto (js/document.createElement "canvas")
                 (-> .-style .-width (set! "100%"))
                 (-> .-style .-height (set! "100%")))]
    (.appendChild card canvas)
    canvas))

(defn resize-canvas [canvas]
  (let [display-width canvas.clientWidth
        display-height canvas.clientHeight]
    (when (or (not= canvas.width display-width)
              (not= canvas.height display-height))
      (set! canvas.width display-width)
      (set! canvas.height display-height))))
