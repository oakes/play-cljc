(ns play-cljc.example-utils
  (:require [play-cljc.core :as c]))

(defn init-example [card]
  (when-let [canvas (.querySelector card "canvas")]
    (.removeChild card canvas))
  (let [canvas (doto (js/document.createElement "canvas")
                 (-> .-style .-width (set! "100%"))
                 (-> .-style .-height (set! "100%")))]
    (.appendChild card canvas)
    (assoc (c/create-game (.getContext canvas "webgl2"))
      :canvas canvas)))

(defn resize-example [{:keys [canvas]}]
  (let [display-width canvas.clientWidth
        display-height canvas.clientHeight]
    (when (or (not= canvas.width display-width)
              (not= canvas.height display-height))
      (set! canvas.width display-width)
      (set! canvas.height display-height))))
