(ns ona.dataview.details
   (:require-macros [cljs.core.async.macros :refer [go]])
   (:require [clojure.string :as s]
             [cljs.core.async :refer [<! chan mult tap put! timeout]]
             [om.core :as om :include-macros true]
             [sablono.core :as html :refer-macros [html]]
             [ona.api.io :as io]
             [ona.dataview.shared :as shared :refer [update-data-on!]]
             [hatti.shared :refer [event-tap event-chan]]
             [hatti.views :refer [details-page]]
             [ona.utils.common-om-components :as common]
             [ona.utils.dom :as dom-utils :refer [click-fn new-container!]]
             [ona.utils.interop :refer [console-log redirect-to-url!]]
             [ona.utils.numeric :refer [pluralize-number]]
             [ona.utils.permissions :as p]
             [ona.utils.remote :as remote]
             [ona.utils.shared-dom :as shared-dom]
             [ona.utils.string :as string-utils]
             [ona.utils.url :as url-utils]))

;; WIRING

(def xls-export-data-type "external_export")

(def media-data-type "media")

(def filetypes
  {:csv "text/csv"
   :xls (str "application/vnd.ms-excel,"
             "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
   :media (str "image/jpeg,image/png,audio/mp3,audio/wav,video/3gp,text/csv,"
               "application/octet-stream,application/zip,application/x-zip,"
                "application/x-zip-compressed")})

;; HELPER FUNCTIONS

(defn filter-metadata
  [value metadata]
  "Filter list of form metadata to get metadata with a specific data_type"
  (filter #(= value (:data_type %)) metadata))

(defn- zebra-url-from-shared
  [{:keys [username project-id dataset-id] :as shared-state} & args]
  "Helper function to make zebra url from shared app-state.
   - (zebra-url-from-shared foo bar baz) will translate to
     (io/make-zebra-url username project-id dataset-id foo bar baz)"
  (apply (partial io/make-zebra-url username project-id dataset-id) args))

;; EVENT HANDLERS

(defn handle-events [app-state shared-state]
  (let [event-chan (event-tap)]
    (go
     (while true
       (let [{:keys [file-id-deleted refresh-info]} (<! event-chan)]
         (when file-id-deleted
           (let [remover (fn [metadata]
                           (remove #(= file-id-deleted (:id %)) metadata))]
             (om/transact! app-state [:metadata] remover)))
         (when refresh-info
           (go
            (let [{:keys [dataset-id auth-token]} shared-state
                  form (-> (io/make-json-url "forms" dataset-id)
                           (io/get-url {} auth-token :no-cache? true)
                           <! :body)
                  new-app-state (merge form {:editing? (:editing? @app-state)})]
              (om/update! app-state new-app-state)))))))))

;; ACTION LISTENERS

(defn delete-on-confirm!
  [filename {:keys [id] :as file} auth-token]
  "Show a confirmation modal, and then delete the given file."
  (let [action #(-> (io/make-json-url "metadata" id)
                    (io/delete-url {} auth-token))
        opts {:text [:div "Are you sure you want to delete "
                     [:span.t-bold.file-name filename]]
              :btn-text "Delete"}]
    (go (when (<! (common/xhr-confirm-modal opts action))
          (put! event-chan {:file-id-deleted id})))))

;; OM COMPONENTS

(def odk-message
  [:p.small "In ODK Collect's Main Menu, press the Menu button. Select General
   Settings, then Configure platform settings. Enter the above as the server.
   For more info, go to " [:a {:href "http://opendatakit.org/use/collect/"}
                           "http://opendatakit.org/use/collect/"]])
(defn odk-enketo-info
  [metadata owner]
  "Render the ODK / Enketo Info modal / widget, depending on permissions."
  (reify
    om/IInitState
    (init-state [_] {:clicked? false})
    om/IRenderState
    (render-state [_ {:keys [clicked?]}]
      (let [{:keys [role]} (om/get-shared owner)
            enketo-info (first (filter-metadata "enketo_url" metadata))
            btn [:a#odk-info-btn.odk-info.pure-button.btn-border.red.right
                 {:on-click #(om/set-state! owner :clicked? true)}
                 "ODK/Enketo Info"]]
        (html
         (when (p/can-add-data? role)
           [:div.container
            btn
            (when clicked?
              [:div.widget-info#odk-info
               [:a.btn-close.right
                {:on-click #(om/set-state! owner :clicked? false)} "×"]
               [:h2.t-center "Submission Options"]
               [:div.cfix
                [:form.pure-form.pure-form-stacked
                 [:h3.t-red "Enketo Webform (URL)"]
                 [:div.info-url#webform-url (:data_value enketo-info)]
                 [:div.spacer.border-bottom]
                 [:h3.t-red "ODK COllect"]
                 [:label "Server:"]
                 [:div.info-url#odk-server remote/forms-host]
                 odk-message]]])]))))))

(defn form-header
  "Form Header; contains project name, public/private, form name."
  [{:keys [title description public metadata]} owner]
  (om/component
   (html
    [:div.dataset-actions.border-bottom
     (om/build odk-enketo-info metadata)
     [:div.container
      [:span.project-name (str (om/get-shared owner [:project-name]) " ")]
      (if public
        [:span.label.grey [:i.fa.fa-eye] " Public "]
        [:span.label.ylw [:i.fa.fa-lock] " Private "])
      [:h3.form-name title]]])))

(def form-download-options
  {"xls" "Download XLSForm"
   "xml" "Download XForm"
   "json" "Download JSONForm"})

(defn form-dropdown
  "Dropdown containing form actions."
  [{:keys [id_string editing?] :as form} owner]
  (om/component
   (let [{:keys [username dataset-id project-id] } (om/get-shared owner)
         fmt->url #(io/make-zebra-url (url-utils/last-url-param (:owner form))
                                      project-id
                                      dataset-id
                                      (str "form." %))
         upload-url (io/make-zebra-url username project-id "new")
         msgs {:error "Form replacement unsuccessful:"
               :done "Form replaced with: "}]
     (html
      [:div.pure-control-group
       [:label {:for "form-source"} "Source"]
       (if editing?
         [:div.infobox
          (om/build common/file-upload-button nil
                    {:opts
                     {:btn-text "Replace form"
                      :pre-existing-filename (str id_string ".xls")
                      :single-upload? true
                      :filetype (:xls filetypes)
                      :input-name "file"
                      :params {"dataset-id" dataset-id}
                      :url upload-url
                      :success-cb #(put! event-chan {:refresh-info true})
                      :messages msgs}})]
         [:div.dropdown.drop-hover
          [:div
           [:a.ellipsis.link-black (str id_string ".xls")]
           [:i.fa.fa-angle-down]]
          [:ul.submenu.no-dot
           (for [[fmt txt] form-download-options]
             [:li (common/download-link (fmt->url fmt) txt)])]])]))))

(def editable-fields [:title :description :downloadable])

(defn settings-form
  "Settings Form, which has a display and an edit mode.
   - Edit mode modifies the form data, which is only committed on 'Save',
     so local state keeps a copy of the editable app-state.
   - :editing? is set in app-state, since it also affects other components."
  [form owner]
  (reify
    om/IInitState
    (init-state [_]
      "Local-state will be a copy of certain values from the form."
      (select-keys form editable-fields))
    om/IRenderState
    (render-state [_ state]
      "Renders the settings-form, for both edit and display mode.
       form cusror corresponds to server values, and contains :editing?
       local-state is used to save the state of inline inputs in edit mode."
      (let [{:keys [title description downloadable id_string editing?]} form
            {:keys [dataset-id auth-token]} (om/get-shared owner)
            ;; save = commit changes to the server
            save! #(go
                    (let [url (io/make-json-url "forms" dataset-id)
                          data (select-keys (om/get-state owner) editable-fields)
                          response (:body (<! (io/patch-url url data auth-token)))]
                      (if (not= id_string (:id_string response))
                        (.alert js/window (str "Something went wrong... "
                                               (:detail response)))
                        (om/update! form response))))
            ;; cancel = reset local-state, set :editing? to false
            cancel! (fn []
                      (om/update! form :editing? false)
                      (doseq [f editable-fields]
                        (om/set-state! owner f (f form))))]
        (html
         (if editing?
           ;; Edit mode uses local-state to get+set inputs
           (let [{:keys [title description downloadable]} state]
             [:div.settings-form
              (om/build form-dropdown form)
              [:form {:id "settings-form" :class "pure-form pure-form-aligned"}
               [:div.pure-control-group
                [:label "Name"]
                [:input {:id "form-name" :ref "form-name" :value title :type "text"
                         :on-change #(om/set-state! owner :title (.. % -target -value))}]]
               [:div.pure-control-group
                [:label "Description"]
                [:textarea {:id "form-description" :value description :ref "form-description"
                            :on-change #(om/set-state! owner :description (.. % -target -value))}]]
               [:div.pure-control-group
                [:label "Form ID"]
                [:span {:class "detail-form-id t-grey"} id_string]]
               [:div.pure-control-group
                [:label "Status"]
                [:span.radio-2
                 [:input {:value true :type "radio" :checked downloadable
                          :on-click #(om/set-state! owner :downloadable true)}]
                 "Active"]
                [:span.radio-2
                 [:input {:value false :type "radio" :checked (not downloadable)
                          :on-click #(om/set-state! owner :downloadable false)}]
                 "Inactive"]]
               [:div.pure-control-group
                [:button {:class "pure-button btn-warning"
                          :on-click (click-fn save!)} "Save"]
                [:button {:class "pure-button"
                          :on-click (click-fn cancel!)} "Cancel"]]]])
           ;; Non-edit mode uses data from the cursor to display data
           [:form {:id "settings-form" :class "pure-form pure-form-aligned" }
            [:fieldset
             [:div.pure-control-group
              [:label "Name"]
              [:span {:class "detail-form-name"} title]]
             [:div.pure-control-group
              [:label "Description"]
              [:span {:class "detail-form-description"} description]]
             [:div.pure-control-group
              [:label "Form ID"]
              [:span {:class "detail-form-id"} id_string]]
             (om/build form-dropdown form)
             [:div.pure-control-group
              [:label {:for "form-status"} "Status"]
              [:span {:class "detail-form-active"}
               (if downloadable "Active" "Inactive")]]]]))))))

(defn media-upload-form
  [_ owner]
  "Om component for uploading Media files, defers to common/file-upload-button"
  (om/component
   (let [{:keys [role]} (om/get-shared owner)
         upload-url (zebra-url-from-shared (om/get-shared owner) "media")
         msgs {:initial [:span.t-small.tip "(jpeg/png/mp3/wav/3gp/csv/zip allowed)"]
               :error [:p.status-msg.error "Media upload unsuccessful!"]}]
     (when (p/can-edit-form? role)
       (om/build common/file-upload-button nil
                 {:opts
                  {:btn-text "Select file to upload"
                   :filetype (:media filetypes)
                   :input-name "media-file"
                   :url upload-url
                   :success-cb #(put! event-chan {:refresh-info true})
                   :messages msgs}})))))

(defn media-list-item
  [{:keys [media-file]} owner]
  "Generate a Media Item list row"
  (om/component
   (let [filename (:data_value media-file)
         {:keys [auth-token role]} (om/get-shared owner)]
     (html
      [:li
       filename
       (when (p/can-edit-form? role)
         [:span.rm-file
          {:on-click #(delete-on-confirm! filename media-file auth-token)}
          [:i.fa.fa-times-circle]])]))))

(defn media-section
  [cursor owner]
  "Form media files"
  (reify
    om/IRender
    (render [_]
      (let [metadata (-> cursor :metadata)
            media-files (filter-metadata media-data-type metadata)]
        (html
          [:ul#media-list.infolist
           [:li
            [:input#toggle-media.actgl {:type "checkbox"}]
            [:label.accordion-toggle {:for "toggle-media"} "Media"]
            [:div.media-count (pluralize-number (count media-files) "File")]
            [:div.accordion-box
             [:span#media-upload-activity.media-list
              (om/build media-upload-form cursor)
              [:div#add-media-container
               [:ul.media-sublist
                (for [file media-files]
                  (om/build media-list-item {:media-file file}))]]]]]])))))

(defn csv-upload-progress-modal
  "Progress modal for csv import.
   Polls the status repeatedly and displays to the user in an unclosable modal.
   Once import finishes, renders a closable modal, and updates the data."
  [cursor owner]
  (reify
    om/IDidMount
    (did-mount [_]
      "In did mount, repeatedly poll progress, put into cursor."
      (let [{:keys [url auth-token]} (om/get-shared owner)
            response-chan #(io/get-url url {} auth-token)
            completed? #(and (:additions @cursor) (:updates @cursor))]
        (go
         (while (not (completed?))
           (<! (timeout 500))
           (om/update! cursor (:body (<! (response-chan))))))))
    om/IRenderState
    (render-state [_ state]
      "Until import has completed, render the progress of the import.
       If import has completed, show a final closable modal and update data."
      (let [{:keys [additions progress updates total]} cursor
            progress (if (= cursor {:JOB_STATUS "PENDING"}) 0 progress)
            completed? (and additions updates)
            {:keys [file-name auth-token dataset-id]} (om/get-shared owner)]
        (when completed?
          (common/ask-for-confirmation!
            {:text [:span
                    "Imported " [:span.t-bold file-name] [:br]
                    (str "Added " additions " records and updated "
                         updates " records.")]
             :not-closable? true :no-cancel? true :btn-text "Ok"})
          (update-data-on! :add {:dataset-id dataset-id :auth-token auth-token}))
        (html
         (when-not completed?
           [:div {:class "row modal-container"}
            [:div {:class "modal modal-dialog modal-confirm"}
             [:div {:class "modal-wrap cfix"}
              [:div {:class "modal-body"}
               [:span
                [:i.fa.fa-spinner.fa-spin]
                " Importing " [:span.t-bold file-name] [:br]
                (str "Added " progress " records, "
                     (- total progress) " records remaining.")]]]]]))))))

(defn csv-import-callback
  "Callback to be called with the response to importing a csv file."
  [{:keys [dataset-id task_id] :as response}]
  (let [url (io/make-url "forms" dataset-id
                         (str "csv_import.json?job_uuid=" task_id))
        el (new-container!)]
    (om/root csv-upload-progress-modal
             (atom response)
             {:target el :shared (merge {:url url} response)})))

(defn upload-csv-form
  [cursor owner]
  "Om component for uploading csv import"
  (om/component
   (let [{:keys [role]} (om/get-shared owner)
         {:keys [num_of_submissions]} cursor
         faq-url "http://help.ona.io/faq/import-data/"
         upload-url (zebra-url-from-shared (om/get-shared owner) "csv-import")
         msgs {:initial (common/tooltip-link
                         {:text "?" :tooltip "What's this?"
                          :url faq-url :class "import-tip"})
               :file-selected [:span.t-red
                               "Are you sure? Data uploads cannot be undone. "
                               [:a {:href faq-url :target "_blank"
                                    :class "t-red t-underline"} "What?! Why?"]]
               :error [:p.status-msg.error "Invalid CSV Import!"]}]
     (html
       (if (zero? num_of_submissions)
         (when (p/can-edit-form? role)
           [:div
            [:div {:class "status-msg error"}
             [:span {:class "t-small tip"}
              "WARNING: CSV uploads are highly experimental.
              Please use with caution as your data may be corrupted.
              We are not responsible if your data is corrupted as a result of using CSV uploads."]]
            (om/build common/file-upload-button nil
                      {:opts
                       {:btn-text "Select file to upload"
                        :filetype (:csv filetypes)
                        :input-name "csv-file"
                        :url upload-url
                        :success-cb csv-import-callback
                        :messages msgs}})])
         [:div {:class "status-msg error"} [:p "CSV data uploads are not allowed for forms with data."]])))))

(defn xls-template-upload-form
  [_ owner]
  "Om component for uploading XLS Reports"
  (om/component
   (let [{:keys [role]} (om/get-shared owner)
         upload-url (zebra-url-from-shared (om/get-shared owner) "xls-report")
         msgs {:error [:p.status-msg.error
                       "Invalid XLS Report, or Template already exists!"]}]
     (when (p/can-edit-form? role)
       (om/build common/file-upload-button nil
                 {:opts
                  {:btn-text "Upload XLS Report Template"
                   :filetype (:xls filetypes)
                   :input-name "xls-file"
                   :url upload-url
                   :success-cb #(put! event-chan {:refresh-info true})
                   :messages msgs}})))))

(defn xls-template-item
  [{:keys [template]} owner]
  "List a given XLS Report Item.
   Needs to provide a way to download both XLS template and Report."
  (reify
    om/IInitState
    (init-state [_]
      "State for the XLS report item contains status data. States can be one of:
       {:job_uuid UUID} - export requested {:export_url URL} - export ready
       {} - neither of the above."
      {})
    om/IRenderState
    (render-state [_ {:keys [job_uuid export_url]}]
     (let [{:keys [dataset-id role auth-token] :as shared-state} (om/get-shared owner)
           filename_link (:data_value template)
           [filename-with-ext report-url] (s/split filename_link #"\|")
           template-token (url-utils/last-url-param report-url)
           filename (s/replace filename-with-ext
                               string-utils/file-extension-regex "")
           tpl-str (str "download-template?filename=" filename-with-ext
                             "&token=" template-token)
           template-url (zebra-url-from-shared shared-state tpl-str)
           report-url (zebra-url-from-shared shared-state
                                             "xls-report"
                                             (:id template)
                                             filename-with-ext)]
       (html
         (cond
           export_url [:div#data-downloads
                       (common/download-link export_url
                                             (str "Download " filename))
                       [:a {:on-click (click-fn #(om/set-state! owner {}))
                            :href "#" :class "t-grey"} " ×"]]
           job_uuid [:div#data-downloads [:span "Preparing " [:b filename] " ..."
                                          (shared-dom/loading-spinner)]]
           :else [:li
                  [:a {:class "xls-template" :href template-url}]
                  [:a {:class "xls-export-link" :href report-url
                       :on-click (click-fn
                                  #(shared/watch-async-export! dataset-id "xls" owner {:meta-id (:id template)}))}
                   [:span.name filename]]
                  (when (p/can-edit-form? role)
                    [:span.rm-file
                     {:on-click #(delete-on-confirm! filename template auth-token)}
                     [:i.fa.fa-times-circle]])]))))))

(defn xls-report-section
  [{:keys [metadata]} owner]
  "Om component for the XLS Report section."
  (om/component
   (let [{:keys [role]} (om/get-shared owner)
         tooltip (common/tooltip-link
                  {:url "http://help.ona.io/faq/what-is-a-xls-report/"
                   :text "?" :tooltip "What's this?" :class ""})
         xls-templates (filter-metadata xls-export-data-type metadata)]
     (html
      [:li
       [:input#toggle-xls-report.actgl {:type "checkbox"}]
       [:label.accordion-toggle {:for "toggle-xls-report"}
        "XLS Report (beta)"]
       [:div
        [:span.xls-report-count
         (pluralize-number (count xls-templates) "Report")]
        tooltip]
       [:div.accordion-box
        [:ul#xls-report-activity.infolist
         [:li.csv-list (om/build xls-template-upload-form nil)
          [:div#xls-report-container
           [:ul.infolist.reports-list
            (for [template xls-templates]
              (om/build xls-template-item {:template template}))]]]]]]))))

(defn data-section
  [cursor owner]
  "Om component for the Data section of the details page."
  (om/component
   (let [{:keys [role]} (om/get-shared owner)
         {:keys [num_of_submissions last_submission_time]} cursor]
     (html
      [:ul#data-info.infolist
       [:li.csv-list
        [:label "Records"]
        [:div.infobox
         (pluralize-number num_of_submissions "Record")
         (str " (last " (.fromNow (js/moment last_submission_time)) ")")]]
       (when (p/can-edit-form? role)
         [:li
          [:label "Import CSV (beta)"]
          [:div.infobox (om/build upload-csv-form cursor)]])
       (om/build xls-report-section cursor)]))))

(defmethod details-page :ona-zebra
  [{:keys [dataset-info]} owner]
  "Om component for the whole details page."
  (reify
    om/IWillMount
    (will-mount [_]
      (handle-events dataset-info (om/get-shared owner)))
    om/IRender
    (render [_]
      (let [{:keys [editing?]} dataset-info
            {:keys [role]} (om/get-shared owner)]
        (html
         [:div#tab-contentsettings
          (om/build form-header dataset-info)
          [:div.container
           [:div.pure-g-r
            [:div.pure-u-3-4
             [:h3.t-red "Form"]]
            (when (and (p/can-edit-form? role) (not editing?))
              [:div.pure-u-1-4
               [:a {:id "edit-form-link" :class "right"
                    :on-click #(om/update! dataset-info :editing? true)}
                [:i.fa.fa-cog] " Edit Form Details"]])]
           (om/build settings-form dataset-info)
           (when-not editing?
             (om/build media-section dataset-info))]
          (when-not editing?
            [:div {:id "data-container" :class "container"}
             [:h3.t-red "Data"]
             (om/build data-section dataset-info)])])))))
