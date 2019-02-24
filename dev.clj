(require
  '[figwheel.main :as figwheel]
  '[dynadoc.core :as dynadoc])

(dynadoc/-main "--port" "5000")
(figwheel/-main "--build" "dev")

