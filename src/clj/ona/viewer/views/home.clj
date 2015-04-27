(ns ona.viewer.views.home
  (:require [hiccup.page :refer [html5]]
            [ona.viewer.views.accounts :as accounts]
            [ona.viewer.views.datasets :as datasets]))

(defn about-page
  "Render the about page"
  []
  (html5
    [:head
     [:title "About Us"]]
    [:body
     [:h1 "This is a lite Zebra"]]))

(defn home-page
  "Render the about page"
  [account]
  (if (:username account)
    (datasets/datasets account)
    (accounts/login-page)))
