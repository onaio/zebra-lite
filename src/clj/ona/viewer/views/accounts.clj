(ns ona.viewer.views.accounts
  (:require [hiccup.page :refer [html5]]))


(defn login-page
  "Renders the login page"
  []
  (html5
    [:head
     [:title "Login"]]
    [:body
     [:h1 "Sorry you'll have to login first!"]]))
