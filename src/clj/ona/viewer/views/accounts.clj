(ns ona.viewer.views.accounts
  (:require [ona.utils.shared-dom :refer [loading-spinner]]
            [ona.viewer.helpers.tags :as tags]
            [ona.viewer.views.template :as template]))


(defn login-page
  "Renders the login page"
  []
  (template/base "Login"
                 (loading-spinner {:class "fullpage-spinner"} "Loading Zebra...")
                 (tags/js-tag "ona.login.init()")))
