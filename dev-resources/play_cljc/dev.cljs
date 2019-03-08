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
