(ns {{name}}.start
  (:require [{{name}}.{{core-name}} :as c]
            [play-cljc.gl.core :as pc]
            [goog.events :as events]))

(defn resize [{:keys [context] :as game}]
  (let [display-width context.canvas.clientWidth
        display-height context.canvas.clientHeight]
    (when (or (not= context.canvas.width display-width)
              (not= context.canvas.height display-height))
      (set! context.canvas.width display-width)
      (set! context.canvas.height display-height))))

(defn game-loop [game]
  (resize game)
  (let [game (c/run game)]
    (js/requestAnimationFrame
      (fn [ts]
        (let [ts (* ts 0.001)]
          (game-loop (assoc game
                            :delta-time (- ts (:total-time game))
                            :total-time ts)))))))

(defn listen-for-mouse [canvas]
  (events/listen js/window "mousemove"
    (fn [event]
      (swap! c/*state
        (fn [state]
          (let [bounds (.getBoundingClientRect canvas)
                x (- (.-clientX event) (.-left bounds))
                y (- (.-clientY event) (.-top bounds))]
            (assoc state :mouse-x x :mouse-y y)))))))

(defonce context
  (let [canvas (js/document.querySelector "canvas")
        context (.getContext canvas "webgl2")
        initial-game (assoc (pc/->game context)
                            :delta-time 0
                            :total-time 0)]
    (listen-for-mouse canvas)
    (c/init initial-game)
    (game-loop initial-game)
    context))

