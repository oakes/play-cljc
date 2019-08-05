(ns play-cljc.gl.example-fonts
  (:require [play-cljc.text :as text]))

(def bitmap-size 512)
(def bitmaps {:firacode (text/->bitmap bitmap-size bitmap-size)
              :roboto (text/->bitmap bitmap-size bitmap-size)})
(def font-height 64)
(def baked-fonts {:firacode (text/->baked-font "ttf/FiraCode-Regular.ttf" font-height (:firacode bitmaps))
                  :roboto (text/->baked-font "ttf/Roboto-Regular.ttf" font-height (:roboto bitmaps))})

(defn load-font-clj [font-key callback]
  (callback (font-key bitmaps) (font-key baked-fonts)))

(defmacro load-font-cljs [font-key callback]
  (let [{:keys [width height] :as bitmap} (font-key bitmaps)]
    `(let [image# (js/Image. ~width ~height)]
       (doto image#
         (-> .-src (set! ~(text/bitmap->data-uri bitmap)))
         (-> .-onload (set! #(~callback {:data image# :width ~width :height ~height} ~(font-key baked-fonts))))))))

