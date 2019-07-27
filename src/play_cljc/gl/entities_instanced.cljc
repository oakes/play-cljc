(ns play-cljc.gl.entities-instanced
  (:refer-clojure :exclude [assoc]))

(defrecord InstancedEntity [instance-count])

(defprotocol IInstancedEntity
  (->instanced-entity [entity instance-count])
  (assoc-instance [entity instanced-entity i]))

(defn assoc [instanced-entity i entity]
  (assoc-instance entity instanced-entity i))

