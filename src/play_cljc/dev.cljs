(ns play-cljc.dev
  (:require play-cljc.examples-2d
            play-cljc.examples-3d
            play-cljc.examples-advanced
            dynadoc.core
            [orchestra-cljs.spec.test :as st]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]))

(st/instrument)
(set! s/*explain-out* expound/printer)

(defn create-canvas [card]
  (when-let [canvas (.querySelector card "canvas")]
    (.removeChild card canvas))
  (let [canvas (doto (js/document.createElement "canvas")
                 (-> .-style .-width (set! "100%"))
                 (-> .-style .-height (set! "100%")))]
    (.appendChild card canvas)
    canvas))
