(ns ona.viewer.views.home
  (:require [hiccup.page :refer [html5]]
            [ona.viewer.views.accounts :as accounts]
            [ona.viewer.views.datasets :as datasets]))

(defn dashboard
  "Render the home page"
  [account]
  (if (:username account)
    (datasets/all account)
    (accounts/login account nil)))
