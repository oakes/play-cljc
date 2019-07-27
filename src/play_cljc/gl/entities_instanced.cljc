(ns play-cljc.gl.entities-instanced
  (:refer-clojure :exclude [assoc]))

(defprotocol IInstance
  (->instanced-entity [entity instance-count]))

(defprotocol IInstanced
  (assoc [instanced-entity i entity]))

