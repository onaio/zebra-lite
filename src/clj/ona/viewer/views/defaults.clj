(ns ona.viewer.views.defaults
  (:require [hiccup.page :refer [html5]]))


(defn about-page
  "Render the about page"
  []
  (html5
    [:head
     [:title "About Us"]]
    [:body
     [:h1 "This is a lite Zebra"]]))

(defn not-found
  "Render page not found"
  []
  (html5
    [:head
     [:title "Page not found"]]
    [:body
     [:h1 "Page not found"]]))

(defn error
  "Render error page"
  []
  (html5
    [:head
     [:title "Service unavailable"]]
    [:body
     [:h1 "Service is temporarily unavailable"]]))

