(ns ona.dataview.base
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [dommy.macros :refer [sel1]])
  (:require [clojure.string :as s]
            [cljs.core.async :refer [<! chan mult tap put!]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [ona.api.io :as io]
            [ona.api.dataset :as api]
            [ona.api.async-export :as export-api]
            [hatti.shared :as hatti-shared]
            [hatti.ona.post-process :as post-process]
            [ona.dataview.shared :as shared]
            [ona.utils.common-om-components :as common]
            [ona.utils.dom :as domutils :refer [click-fn]]
            [hatti.ona.forms :as f :refer [flatten-form]]
            [ona.utils.interop :refer [json->cljs format console-log]]
            [ona.utils.numeric :refer [pluralize-number]]
            [ona.utils.shared-dom :as shared-dom]
            [ona.utils.walkthrough :as walkthrough-utils]
            ;; Hatti views; need to also include all namespaces where methods are defined
            [hatti.views :refer [tabbed-dataview dataview-actions]]
            [hatti.views.dataview]
            [ona.dataview.details]
            [ona.dataview.single-submission]))

;;; CONFIG data that should sit at "client", not "library" level

(def ona-mapbox-tiles
  [{:url "//{s}.tiles.mapbox.com/v3/ona.cc25710e/{z}/{x}/{y}.png"
    :name "Ona Outdoors"
    :attribution "Data &copy; <a href=\"http://osm.org/copyright\">
                 OpenStreetMap</a> contributors. Imagery &copy; Mapbox."}
   {:url "//{s}.tiles.mapbox.com/v3/ona.jjob2254/{z}/{x}/{y}.png"
    :name "Ona Streets"
    :attribution "Data &copy; <a href=\"http://osm.org/copyright\">
                 OpenStreetMap</a> contributors. Imagery &copy; Mapbox."}
   {:url "//{s}.tiles.mapbox.com/v3/ona.jfi8jjna/{z}/{x}/{y}.png"
    :name "Mapbox Satellite"
    :attribution "Data &copy; <a href=\"http://osm.org/copyright\">
                 OpenStreetMap</a> contributors. Imagery &copy; Mapbox."}
   {:url "//{s}.tiles.mapbox.com/v3/ona.jjoa711a/{z}/{x}/{y}.png"
    :name "Ona Plain"
    :attribution "Data &copy; <a href=\"http://osm.org/copyright\">
                 OpenStreetMap</a> contributors. Imagery &copy; Mapbox."}
    {:url "//{s}.tiles.mapbox.com/v3/ona.kop8en8f/{z}/{x}/{y}.png"
    :name "Ona Contrast"
    :attribution "Data &copy; <a href=\"http://osm.org/copyright\">
                 OpenStreetMap</a> contributors. Imagery &copy; Mapbox."}
   {:url "//{s}.tiles.mapbox.com/v3/ona.kf6g1a73/{z}/{x}/{y}.png"
    :name "Ona Night"
    :attribution "Data &copy; <a href=\"http://osm.org/copyright\">
                 OpenStreetMap</a> contributors. Imagery &copy; Mapbox."}
   {:url "http://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png"
    :name "Humanitarian OpenStreetMap Team"
    :attribution "&copy;  <a href=\"http://osm.org/copyright\">
                  OpenStreetMap Contributors.</a>
                  Tiles courtesy of
                  <a href=\"http://hot.openstreetmap.org/\">
                  Humanitarian OpenStreetMap Team</a>."}])

;; EDIT and DELETE buttons

(defn- open-delete-confirmation
  "Show a confirmation for deletion, making a delete request if user accepts."
  [dataset-id auth-token instance-id]
  (let [url (io/make-url "data" dataset-id (str instance-id ".json"))
        action #(io/delete-url url {} auth-token)
        opts {:text [:div
                     [:p "Are you sure that you want to delete this record?"]
                     [:p {:class "t-red"} "There is no way to undo deletion."]]
              :header "Confirm Delete" :btn-text "Confirm"}]
    (go (when (<! (common/xhr-confirm-modal opts action))
          (put! hatti-shared/event-chan {:submission-unclicked instance-id})
          (shared/update-data-on! :delete {:instance-id instance-id})))))

(def non-async-formats #{"json" "csvzip"})

(defn download-formats [form]
  (remove nil? ["csv"
                "csvzip"
                "json"
                (when (some f/osm? form) "osm")
                ;; "kml" ; KML needs to be tested on API side
                ;; "sav" ; SAV is apparently broken on the API side
                "xlsx"]))

(defn download-menu [_ owner]
  (reify
    om/IInitState
    (init-state [_]
      "State for the download menu contains status data. States can be one of:
       {:job_uuid UUID} - export requested {:export_url URL} - export ready
       {} - neither of the above."
      {})
    om/IRenderState
    (render-state [_ {:keys [export_url job_uuid fmt]}]
      (let [{:keys [username project-id dataset-id]} (om/get-shared owner)
            formats (download-formats (om/get-shared owner :flat-form))
            direct-url #(io/make-zebra-url username project-id dataset-id
                                           (str "download." %))]
        (html
         (cond
          export_url [:div#data-downloads
                      (common/download-link (direct-url (str fmt "?async=true"))
                                            (str "Download " (s/upper-case fmt)))
                      [:a {:on-click (click-fn #(om/set-state! owner {}))
                           :href "#" :class "t-grey"} " ×"]]
          job_uuid [:div#data-downloads (shared-dom/loading-spinner)]
          :else
          [:div#data-downloads.dropdown.drop-hover
           [:div [:a {:ref "#source" :title "Download Data"}
                  [:span.icon-data.download] [:i.fa.fa-angle-down]]]
           [:ul.submenu.no-dot
            (for [fmt formats]
              [:li
               (if (non-async-formats fmt)
                 (common/download-link (direct-url fmt) (str "Download " (s/upper-case fmt)))
                 [:a {:on-click
                      (click-fn
                       #(shared/watch-async-export! dataset-id fmt owner))}
                  (format "Prepare %s export" (s/upper-case fmt))])])]]))))))

(defmethod dataview-actions :ona-zebra
  [dataset-id owner]
  (om/component
   (html
    (let [{:keys [role auth-token]} (om/get-shared owner)]
      [:div#data-actions
       (om/build download-menu nil)
       " \u00A0 " ; &nbsp;
       [:span.data-submission-button-container
        (om/build common/add-submission-button dataset-id
                  {:opts {:role role :auth-token auth-token
                          :class "enter-data pure-button btn-border"}})]]))))

;; WIRING

(defn init-views
  [data-atom delete-record! role chart-get shared-state]
  "Render the map, chart and table views."
  (om/root tabbed-dataview
           data-atom
           {:shared shared-state
            :opts {:delete-record! delete-record!
                   :role role
                   :chart-get chart-get}
            :target (domutils/by-id "dataset-view")}))

(defn download-and-process-osm-data!
  [auth-token dataset-id form]
  (when (some f/osm? form)
    (go
     (let [osm-xml-chan (export-api/get-async-export-data
                        auth-token dataset-id "osm" :raw-get)
           osm-xml (:body (<! osm-xml-chan))]
       (post-process/integrate-osm-data! hatti-shared/app-state form osm-xml)))))

(defn ^:export init [dataset-id username auth-token]
  "This function is called directly in the html of the data page;
   it downloads data+form, and wires up the table-page and map-page components."
  (go
   (let [auth-token (<! (io/validate-token auth-token username))
         delete-record! (partial open-delete-confirmation dataset-id auth-token)
         chart-url #(io/make-url "charts" (str dataset-id
                                               ".json?field_name=" %))
         chart-get #(io/get-url (chart-url %) {} auth-token)
         ;; We want requests in parallel, so defined chans before first <!
         ;; Defining chans makes the request, First take (<!) waits for result
         form-chan (api/form auth-token dataset-id)
         data-chan (api/data auth-token dataset-id :raw? true)
         form-desc-chan (api/metadata auth-token dataset-id)
         ;; Define all chans before first take (<!) = parallel requests
         form (-> (<! form-chan) :body flatten-form)
         form-desc (-> (<! form-desc-chan) :body)
         data (-> (<! data-chan) :body json->cljs)
         shared-state {:dataset-id dataset-id
                       :username username
                       :auth-token auth-token
                       :map-config {:mapbox-tiles ona-mapbox-tiles
                                    :include-google-maps? true}
                       :flat-form form
                       :view-type :ona-zebra}]
     (hatti-shared/update-app-data! hatti-shared/app-state data :rerank? true)
     (hatti-shared/transact-app-state! hatti-shared/app-state
                                       [:dataset-info]
                                       (fn [_] form-desc))
     (init-views hatti-shared/app-state delete-record! role chart-get shared-state)
     (post-process/integrate-attachments! hatti-shared/app-state form)
     (walkthrough-utils/launch-data-view-walkthrough auth-token)
     (download-and-process-osm-data! auth-token dataset-id form))))
