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

(defn start []
  (st/instrument)
  (set! s/*explain-out* expound/printer)
  (let [window (start/->window)
        game (pc/->game (:handle window))
        paravim-utils (paravim.start/init game)
        *focus-on-game? (atom true)]
    (extend-type Window
      start/Events
      (on-mouse-move [{:keys [handle]} xpos ypos]
        (if @*focus-on-game?
          (start/on-mouse-move! handle xpos ypos)
          (paravim.start/on-mouse-move! paravim-utils handle xpos ypos)))
      (on-mouse-click [{:keys [handle]} button action mods]
        (if @*focus-on-game?
          (start/on-mouse-click! handle button action mods)
          (paravim.start/on-mouse-click! paravim-utils handle button action mods)))
      (on-key [{:keys [handle]} keycode scancode action mods]
        (if (and (= action GLFW/GLFW_PRESS)
                 (= keycode GLFW/GLFW_KEY_ESCAPE)
                 (= (paravim.core/get-mode) 'NORMAL))
          (swap! *focus-on-game? not)
          (if @*focus-on-game?
            (start/on-key! handle keycode scancode action mods)
            (paravim.start/on-key! paravim-utils handle keycode scancode action mods))))
      (on-char [{:keys [handle]} codepoint]
        (if @*focus-on-game?
          (start/on-char! handle codepoint)
          (paravim.start/on-char! paravim-utils handle codepoint)))
      (on-resize [{:keys [handle]} width height]
        (start/on-resize! handle width height)
        (paravim.start/on-resize! paravim-utils handle width height))
      (on-tick [this game]
        (cond-> (c/tick game)
                (not @*focus-on-game?)
                paravim.core/tick)))
    (start/start game window)))

