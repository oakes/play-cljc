(ns play-cljc.transforms)

(defprotocol IProject
  (project [entity attrs]))

(defprotocol ITranslate
  (translate [entity attrs]))

(defprotocol IScale
  (scale [entity attrs]))

(defprotocol IRotate
  (rotate [entity attrs]))

(defprotocol ICamera
  (camera [entity attrs]))

(extend-type #?(:clj Object :cljs default)
  ITranslate
  (translate [entity attrs] entity)
  IScale
  (scale [entity attrs] entity)
  IProject
  (project [entity attrs] entity)
  IRotate
  (rotate [entity attrs] entity)
  ICamera
  (camera [entity attrs] entity))

(defprotocol ILookAt
  (look-at [camera attrs]))

