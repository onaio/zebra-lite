(ns ona.viewer.views.datasets
  (:require [hiccup.page :refer [html5 include-js]]
            [ona.utils.shared-dom :refer [loading-spinner]]
            [ona.utils.tags :as tags]
            [ona.viewer.views.template :as template]))

(defn list-datasets
  [account]
  (template/base "Home"
                 [:div {:id "content"}
                  [:div {:id "main-menu"}]
                   [:div {:id "dataset-container"}
                    (loading-spinner {:class "fullpage-spinner"} "Loading Forms...")]]
                 (tags/js-tag (format
                                "ona.dataset.init(\"%s\", \"%s\");"
                                (:username account) (:temp_token account)))))

(defn dataview
  [account dataset-id]
  (template/base "Home"
                 [:div {:id "content"}
                  [:div {:id "dataset-view"}
                   (loading-spinner {:class "fullpage-spinner"} "Loading Data...")]]
                 (tags/js-tag (format "ona.dataview.base.init(\"%s\",\"%s\",\"%s\");\n"
                                      dataset-id (:username account) (:temp_token account) "owner"))))
