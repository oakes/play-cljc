(ns {{name}}.start-dev
  (:require [{{name}}.start :as start]
            [{{name}}.{{core-name}} :as c]
            [orchestra.spec.test :as st]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]
            [play-cljc.gl.core :as pc]
            [paravim.start]
            [paravim.core])
  (:import [org.lwjgl.glfw GLFW]
           [{{project_name}}.start Window]))

(defn start-paravim [game]
  (let [paravim-utils (paravim.start/init game)
        *focus-on-game? (atom true)
        *last-tick (atom 0)]
    (extend-type Window
      start/Events
      (on-mouse-move [{:keys [handle]} xpos ypos]
        (if @*focus-on-game?
          (try
            (start/on-mouse-move! handle xpos ypos)
            (catch Exception e (.printStackTrace e)))
          (paravim.start/on-mouse-move! paravim-utils handle xpos ypos)))
      (on-mouse-click [{:keys [handle]} button action mods]
        (if @*focus-on-game?
          (try
            (start/on-mouse-click! handle button action mods)
            (catch Exception e (.printStackTrace e)))
          (paravim.start/on-mouse-click! paravim-utils handle button action mods)))
      (on-key [{:keys [handle]} keycode scancode action mods]
        (if (and (= action GLFW/GLFW_PRESS)
                 (= keycode GLFW/GLFW_KEY_ESCAPE)
                 (= (paravim.core/get-mode) 'NORMAL))
          (swap! *focus-on-game? not)
          (if @*focus-on-game?
            (try
              (start/on-key! handle keycode scancode action mods)
              (catch Exception e (.printStackTrace e)))
            (paravim.start/on-key! paravim-utils handle keycode scancode action mods))))
      (on-char [{:keys [handle]} codepoint]
        (if @*focus-on-game?
          (try
            (start/on-char! handle codepoint)
            (catch Exception e (.printStackTrace e)))
          (paravim.start/on-char! paravim-utils handle codepoint)))
      (on-resize [{:keys [handle]} width height]
        (try
          (start/on-resize! handle width height)
          (catch Exception e (.printStackTrace e)))
        (paravim.start/on-resize! paravim-utils handle width height))
      (on-tick [this game]
        (cond-> (try
                  (c/tick game)
                  (catch Exception e
                    (let [current-ms (System/currentTimeMillis)]
                      (when (> (- current-ms @*last-tick) 1000)
                        (reset! *last-tick current-ms)
                        (.printStackTrace e)))
                    game))
                (not @*focus-on-game?)
                paravim.core/tick)))))

(defn start []
  (st/instrument)
  (set! s/*explain-out* expound/printer)
  (let [window (start/->window)
        game (pc/->game (:handle window))]
    (try
      (start-paravim game)
      (catch Throwable e (.printStackTrace e)))
    (start/start game window)))

