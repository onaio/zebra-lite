(ns ona.viewer.views.home
  (:require [hiccup.page :refer [html5]]))

(defn about-page
  "Render the about page"
  []
  (html5
    [:head
     [:title "About Us"]]
    [:body
     [:h1 "This is a lite Zebra"]]))

#_(defn index-page []
  (html5
    [:head
      [:title "Hello World"]]
    [:body
      [:h1 "Hello World"]]))