(ns ona.viewer.views.datasets
  (:require [hiccup.page :refer [html5 include-js]]
            [ona.utils.shared-dom :refer [loading-spinner]]
            [ona.utils.tags :as tags]
            [ona.viewer.views.template :as template]))

(defn list-datasets
  [account]
  (template/base "Home"
                 [:div {:id "content"}
                   [:div {:id "dataset-container"}
                    (loading-spinner {:class "fullpage-spinner"} "Loading Forms...")]]
                 (tags/js-tag (format
                                "ona.dataset.init(\"%s\", \"%s\");"
                                (:username account) (:temp_token account)))))
