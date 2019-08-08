(ns {{name}}.start
  (:require [{{name}}.{{core-name}} :as c]
            [play-cljc.gl.core :as pc]
            [goog.events :as events])
  (:require-macros [{{name}}.music :refer [build-for-cljs]]))

(defn resize [{:keys [context] :as game}]
  (let [display-width context.canvas.clientWidth
        display-height context.canvas.clientHeight]
    (when (or (not= context.canvas.width display-width)
              (not= context.canvas.height display-height))
      (set! context.canvas.width display-width)
      (set! context.canvas.height display-height))))

(defn game-loop [game]
  (resize game)
  (let [game (c/tick game)]
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

(defn keycode->keyword [keycode]
  (condp = keycode
    37 :left
    39 :right
    38 :up
    nil))

(defn listen-for-keys []
  (events/listen js/window "keydown"
    (fn [event]
      (when-let [k (keycode->keyword (.-keyCode event))]
        (swap! c/*state update :pressed-keys conj k))))
  (events/listen js/window "keyup"
    (fn [event]
      (when-let [k (keycode->keyword (.-keyCode event))]
        (swap! c/*state update :pressed-keys disj k)))))

;; start the game

(defonce context
  (let [canvas (js/document.querySelector "canvas")
        context (.getContext canvas "webgl2")
        initial-game (assoc (pc/->game context)
                            :delta-time 0
                            :total-time 0)]
    (listen-for-mouse canvas)
    (listen-for-keys)
    (c/init initial-game)
    (game-loop initial-game)
    context))

;; build music, put it in the audio tag, and make the button toggle it on and off

(defonce play-music? (atom false))

(defonce audio (js/document.querySelector "#audio"))
(set! (.-src audio) (build-for-cljs))
(when @play-music? (.play audio))

(defonce button (js/document.querySelector "#audio-button"))
(set! (.-onclick button)
      (fn [e]
        (if (swap! play-music? not)
          (.play audio)
          (.pause audio))))

