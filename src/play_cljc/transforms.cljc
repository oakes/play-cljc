(ns play-cljc.transforms)

(defprotocol IProject
  (project
    [entity width height]
    [entity left right bottom top near far]
    [entity field-of-view aspect near far]))

(defprotocol ITranslate
  (translate [entity x y] [entity x y z]))

(defprotocol IScale
  (scale [entity x y] [entity x y z]))

(defprotocol IRotate
  (rotate [entity angle] [entity angle axis]))

(defprotocol ICamera
  (camera [entity camera]))

(defprotocol IColor
  (color [entity rgba]))

(extend-type #?(:clj Object :cljs default)
  IProject
  (project
    ([entity width height] entity)
    ([entity left right bottom top near far] entity)
    ([entity field-of-view aspect near far] entity))
  ITranslate
  (translate
    ([entity x y] entity)
    ([entity x y z] entity))
  IScale
  (scale
    ([entity x y] entity)
    ([entity x y z] entity))
  IRotate
  (rotate
    ([entity angle] entity)
    ([entity angle axis] entity))
  ICamera
  (camera [entity camera] entity)
  IColor
  (color [entity rgba] entity))

(defprotocol ILookAt
  (look-at [camera target up]))

