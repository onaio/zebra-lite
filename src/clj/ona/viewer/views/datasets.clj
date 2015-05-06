(ns ona.viewer.views.datasets
  (:require [hiccup.page :refer [html5]]
            [ona.utils.shared-dom :refer [loading-spinner]]
            [ona.utils.tags :as tags]
            [ona.viewer.views.template :as template]))

(defn datasets
  "Renders the page with forms"
  [account]
  (template/base "Home"
                 (loading-spinner {:class "fullpage-spinner"} "Loading Forms...")
                 (tags/js-tag "ona.datasets.init()")))
