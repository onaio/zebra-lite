(ns ona.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [milia.api.io :as io]))


(defroute "/login" {:as params}
          (js/console.log (str "hit me!")))
