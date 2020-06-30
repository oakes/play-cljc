(ns play-cljc.transforms
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(s/def ::width any?)
(s/def ::height any?)
(s/def ::left any?)
(s/def ::right any?)
(s/def ::bottom any?)
(s/def ::top any?)
(s/def ::near any?)
(s/def ::far any?)
(s/def ::field-of-view any?)
(s/def ::aspect any?)
(s/def ::x any?)
(s/def ::y any?)
(s/def ::z any?)
(s/def ::angle any?)
(s/def ::axis any?)
(s/def ::target any?)
(s/def ::up any?)

(s/def ::project (s/or
                   :2d (s/keys :req-un [::width ::height])
                   :orthographic (s/keys :req-un [::left ::right ::bottom ::top ::near ::far])
                   :perspective (s/keys :req-un [::field-of-view ::aspect ::near ::far])))

(defprotocol IProject
  (project
    [entity width height]
    [entity left right bottom top near far]
    [entity field-of-view aspect near far]))

(s/def ::translate (s/or
                     :3d (s/keys :req-un [::x ::y ::z])
                     :2d (s/keys :req-un [::x ::y])))

(defprotocol ITranslate
  (translate [entity x y] [entity x y z]))

(s/def ::scale (s/or
                 :3d (s/keys :req-un [::x ::y ::z])
                 :2d (s/keys :req-un [::x ::y])))

(defprotocol IScale
  (scale [entity x y] [entity x y z]))

(s/def ::rotate (s/or
                  :3d (s/keys :req-un [::angle ::axis])
                  :2d (s/keys :req-un [::angle])))

(defprotocol IRotate
  (rotate [entity angle] [entity angle axis]))

(defprotocol ICamera
  (camera [entity camera]))

(defprotocol IInvert
  (invert [entity camera]))

(defprotocol IColor
  (color [entity rgba]))

(defprotocol ICrop
  (crop [entity x y width height]))

(s/def ::look-at (s/keys :req-un [::target ::up]))

(defprotocol ILookAt
  (look-at [camera target up]))

(defn parse [spec content]
  (let [res (s/conform spec content)]
    (if (= ::s/invalid res)
      (throw (ex-info (expound/expound-str spec content) {}))
      res)))

(defmulti transform-entity*
  "Work in progress! Subject to change/break in future releases."
  (fn [transform-name transform-args]
    transform-name))

(defmethod transform-entity* :project [_ args]
  (let [[kind args] (parse ::project args)]
    (case kind
      :2d `(project ~(:width args) ~(:height args))
      :orthographic `(project
                       ~(:left args) ~(:right args) ~(:bottom args)
                       ~(:top args) ~(:near args) ~(:far args))
      :perspective `(project
                      ~(:field-of-view args) ~(:aspect args)
                      ~(:near args) ~(:far args)))))

(defmethod transform-entity* :translate [_ args]
  (let [[kind args] (parse ::translate args)]
    (case kind
      :3d `(translate ~(:x args) ~(:y args) ~(:z args))
      :2d `(translate ~(:x args) ~(:y args)))))

(defmethod transform-entity* :rotate [_ args]
  (let [[kind args] (parse ::rotate args)]
    (case kind
      :3d `(rotate ~(:angle args) ~(:axis args))
      :2d `(rotate ~(:angle args)))))

(defmethod transform-entity* :scale [_ args]
  (let [[kind args] (parse ::scale args)]
    (case kind
      :3d `(scale ~(:x args) ~(:y args) ~(:z args))
      :2d `(scale ~(:x args) ~(:y args)))))

(defmethod transform-entity* :color [_ args]
  `(color ~args))

(defmethod transform-entity* :camera [_ args]
  `(camera ~args))

(defmethod transform-entity* :invert [_ args]
  `(invert ~args))

(defmethod transform-entity* :look-at [_ args]
  (let [args (parse ::look-at args)]
    `(look-at ~(:target args) ~(:up args))))

(defn transform-entity
  "Work in progress! Subject to change/break in future releases."
  [entity transforms]
  (concat ['-> entity]
    (map (fn [[name args]]
           (transform-entity* name args))
      (partition 2 transforms))))

(s/def ::subcontent (s/cat
                      :name keyword?
                      :args map?
                      :content (s/* ::content)))

(s/def ::content (s/or
                   :subcontent ::subcontent
                   :entity any?))

(defn transform
  "Work in progress! Subject to change/break in future releases."
  ([content]
   (transform (parse ::content content) [] []))
  ([[content-type content-val] entities transforms]
   (case content-type
     :subcontent
     (reduce
       (fn [entities content]
         (transform content entities (into transforms ((juxt :name :args) content-val))))
       entities
       (:content content-val))
     :entity
     (conj entities (transform-entity content-val transforms)))))

