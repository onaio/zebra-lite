(ns ona.viewer.views.datasets
  (:require [hiccup.page :refer [html5]]))

(defn datasets
  [account]
  (html5
    [:head
     [:title "Home"]]
    [:body
     [:h1 (str "Welcome! "  (:username account)". We'll load your forms here.")]]))
