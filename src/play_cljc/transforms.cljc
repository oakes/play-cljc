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

(defn parse [spec content]
  (let [res (s/conform spec content)]
    (if (= ::s/invalid res)
      (throw (ex-info (expound/expound-str spec content) {}))
      res)))

(defn transform-attrs [{project-args :project
                        translate-args :translate
                        rotate-args :rotate
                        scale-args :scale
                        color-args :color}]
  (cond-> []
          project-args
          (conj
            (let [[kind args] (parse ::project project-args)]
              (case kind
                :2d `(project ~(:width args) ~(:height args))
                :orthographic `(project
                                 ~(:left args) ~(:right args) ~(:bottom args)
                                 ~(:top args) ~(:near args) ~(:far args))
                :perspective `(project
                                ~(:field-of-view args) ~(:aspect args)
                                ~(:near args) ~(:far args)))))
          translate-args
          (conj
            (let [[kind args] (parse ::translate translate-args)]
              (case kind
                :3d `(translate ~(:x args) ~(:y args) ~(:z args))
                :2d `(translate ~(:x args) ~(:y args)))))
          rotate-args
          (conj
            (let [[kind args] (parse ::rotate rotate-args)]
              (case kind
                :3d `(rotate ~(:angle args) ~(:axis args))
                :2d `(rotate ~(:angle args)))))
          scale-args
          (conj
            (let [[kind args] (parse ::scale scale-args)]
              (case kind
                :3d `(scale ~(:x args) ~(:y args) ~(:z args))
                :2d `(scale ~(:x args) ~(:y args)))))
          color-args
          (conj `(color ~color-args))))

(defn transform
  ([content]
   (transform content []))
  ([content parent-attrs]
   (->> content
        (reduce
          (fn [{:keys [entities attrs] :as m} item]
            (cond
              (vector? item) (update m :entities into (transform item attrs))
              (map? item) (update m :attrs conj item)
              :else (update m :entities conj
                      (concat ['-> item]
                        (mapcat transform-attrs attrs)))))
          {:entities []
           :attrs parent-attrs})
        :entities)))

