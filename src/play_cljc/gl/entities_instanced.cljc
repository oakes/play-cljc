(ns play-cljc.gl.entities-instanced
  (:refer-clojure :exclude [conj]))

(defrecord InstancedEntity [])

(defprotocol ICreateInstancedEntity
  (->instanced-entity [entity]))

(defprotocol IConjInstance
  (conj-instance [entity instanced-entity]))

(defn conj [instanced-entity entity]
  (conj-instance entity instanced-entity))

