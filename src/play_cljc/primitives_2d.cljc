(ns play-cljc.primitives-2d
  "Low level 2D shape data")

(def rect
  ;; x1 y1, x2 y1, x1 y2, x1 y2, x2 y1, x2 y2
  [0 0, 1 0, 0 1, 0 1, 1 0, 1 1])

