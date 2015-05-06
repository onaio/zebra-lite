(ns ona.dataview.single-submission
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.string :as string]
            [om.core :as om :include-macros true]
            [ona.utils.dom :refer [click-fn]]
            [ona.utils.interop :refer [format]]
            [ona.utils.permissions :refer [can-edit-data? can-delete-data?]]
            [sablono.core :as html :refer-macros [html]]
            [hatti.views :refer [edit-delete print-xls-report-btn]]
            [ona.api.io :as io]
            [ona.utils.url :refer [last-url-param]]))

(def xls-export-data-type "external_export")

(defn- filter-xls-export
  [metadata]
  (let [xls-reports (filter #(= (:data_type %) xls-export-data-type) metadata)]
    (for [{:keys [data_value id]} xls-reports]
      (let [[filename report-url] (string/split data_value #"\|")
            template-token (last-url-param report-url)]
        {:filename filename :id id :template-token template-token}))))

(defmethod print-xls-report-btn :ona-zebra
  [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [instance-id dataset-info]} cursor
            {:keys [formid metadata project owner]} dataset-info
            xls-reports (filter-xls-export metadata)
            report-url (fn [meta-id filename]
                         (str "/" (last-url-param owner)
                              "/" (last-url-param project)
                              "/" formid
                              "/xls-report"
                              "/" meta-id
                              "/" filename
                              "?data-id=" instance-id))]
      (html
        (when-not (-> xls-reports count zero?)
          [:div {:class "drop-hover pure-button" :id "print-xls-report"}
           [:a {:href "#"}  "Download XLS Report " [:i.fa.fa-angle-down]]
           [:ul {:class "submenu no-dot"}
            (for [{:keys [filename id]} xls-reports]
              [:li [:a {:href (report-url id filename)}
                    [:i.fa.fa-file] " " filename]])]]))))))

(defmethod edit-delete :ona-zebra
  [instance-id owner {:keys [delete-record!]}]
  (om/component
   (let [{:keys [dataset-id role]} (om/get-shared owner)
         edit-url (io/make-zebra-url
                   (format "webform?instance-id=%s&dataset-id=%s"
                           instance-id dataset-id))
         pre-text " or "]
     (html
      (when (can-edit-data? role)
        [:span ;; _Edit_ or _Delete_
         [:br]
         [:span.edit
          [:a {:href edit-url :target "_blank"} "Edit"]]
         (when (can-delete-data? role)
           [:span.delete pre-text
            [:a {:on-click (click-fn #(delete-record! instance-id))
                 :href "#"} "Delete"]])])))))
