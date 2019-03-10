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

(defn transform-attrs [{project-args :project
                        translate-args :translate
                        rotate-args :rotate
                        scale-args :scale
                        color-args :color}]
  (cond-> []
          project-args
          (conj
            (let [{:keys [width height]} project-args]
              `(project ~width ~height)))
          translate-args
          (conj
            (let [{:keys [x y z]} translate-args]
              (if z
                `(translate ~x ~y ~x)
                `(translate ~x ~y))))
          rotate-args
          (conj
            (let [{:keys [angle axis]} rotate-args]
              (if axis
                `(rotate ~angle ~axis)
                `(rotate ~angle))))
          scale-args
          (conj
            (let [{:keys [x y z]} scale-args]
              (if z
                `(scale ~x ~y ~x)
                `(scale ~x ~y))))
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

