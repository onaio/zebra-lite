(ns ona.login
 (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                  [dommy.macros :refer [sel sel1]])
 (:require [cljs.core.async :refer [<! chan mult tap put!]]
           [om.core :as om :include-macros true]
           [sablono.core :refer-macros [html]]))

(defn ^:export init []
  (.log js/console "Hello zebra lite"))
