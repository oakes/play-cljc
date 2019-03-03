(ns play-cljc.example-utils
  (:require [play-cljc.core :as c]
            [play-cljc.utils :as u]
            [goog.events :as events]))

(defn init-example [card]
  (when-let [canvas (.querySelector card "canvas")]
    (.removeChild card canvas))
  (let [canvas (doto (js/document.createElement "canvas")
                 (-> .-style .-width (set! "100%"))
                 (-> .-style .-height (set! "100%")))
        gl (.getContext canvas "webgl2")]
    (.appendChild card canvas)
    (assoc (c/create-game gl) :canvas canvas)))

(defn resize-example [{:keys [canvas]}]
  (let [display-width canvas.clientWidth
        display-height canvas.clientHeight]
    (when (or (not= canvas.width display-width)
              (not= canvas.height display-height))
      (set! canvas.width display-width)
      (set! canvas.height display-height))))

(defn listen-for-mouse [{:keys [canvas tx ty] :or {tx 0 ty 0}} callback]
  (events/listen js/window "mousemove"
    (fn [event]
      (let [bounds (.getBoundingClientRect canvas)
            x (- (.-clientX event) (.-left bounds) tx)
            y (- (.-clientY event) (.-top bounds) ty)
            rx (/ x (.-width bounds))
            ry (/ y (.-height bounds))
            r (Math/atan2 rx ry)
            cx (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
            cy (- (.-height bounds)
                  (- (.-clientY event) (.-top bounds)))
            cr (-> (/ cx (.-width bounds))
                   (* 360)
                   u/deg->rad)]
        (callback {:x x :y y :rx rx :ry ry :r r :cx cx :cy cy :cr cr})))))

