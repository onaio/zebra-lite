(ns ona.viewer.views.accounts
  (:require [hiccup.page :refer [html5]]
            [ona.viewer.helpers.tags :as tags]
            [ona.viewer.views.template :as template]))


(defn login-page
  "Renders the login page"
  []
  (template/base "Login"
                 "This is the login page"
                 (tags/js-tag "ona.login.init()")))
