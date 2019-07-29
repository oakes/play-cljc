(ns play-cljc.instances
  (:refer-clojure :exclude [assoc]))

(defprotocol IInstance
  (->instanced-entity [entity]))

(defprotocol IInstanced
  (assoc [instanced-entity i entity]))

