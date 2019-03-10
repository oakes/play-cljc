(ns {{name}}.start-dev
  (:require [{{name}}.start]
            [nightlight.repl-server]
            [orchestra-cljs.spec.test :as st]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]))

(st/instrument)
(set! s/*explain-out* expound/printer)

