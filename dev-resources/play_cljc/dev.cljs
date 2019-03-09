(ns play-cljc.dev
  (:require play-cljc.gl.examples-2d
            play-cljc.gl.examples-3d
            play-cljc.gl.examples-advanced
            dynadoc.core
            [orchestra-cljs.spec.test :as st]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]))

(st/instrument)
(set! s/*explain-out* expound/printer)
