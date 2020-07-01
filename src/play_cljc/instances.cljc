(ns play-cljc.instances
  (:refer-clojure :exclude [assoc dissoc]))

(defprotocol IInstance
  (->instanced-entity [entity] "Returns an instanced version of the given entity."))

(defprotocol IInstanced
  (assoc [instanced-entity i entity]
         "Adds an entity to the instanced entity at the given index.")
  (dissoc [instanced-entity i]
          "Removes an entity from the instanced entity at the given index."))

