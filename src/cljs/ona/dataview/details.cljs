(ns ona.dataview.details
   (:require-macros [cljs.core.async.macros :refer [go]])
   (:require [clojure.string :as s]
             [cljs.core.async :refer [<! chan mult tap put! timeout]]
             [om.core :as om :include-macros true]
             [sablono.core :as html :refer-macros [html]]
             [hatti.shared :refer [event-tap event-chan]]
             [hatti.views :refer [details-page]]
             [ona.utils.interop :refer [console-log redirect-to-url!]]
             [ona.utils.numeric :refer [pluralize-number]]))

;; OM COMPONENTS

(defn form-header
  "Form Header; contains project name, public/private, form name."
  [{:keys [title public]} owner]
  (om/component
   (html
    [:div.dataset-actions.border-bottom
     [:div.container
      [:span.project-name (str (om/get-shared owner [:project-name]) " ")]
      (if public
        [:span.label.grey [:i.fa.fa-eye] " Public "]
        [:span.label.ylw [:i.fa.fa-lock] " Private "])
      [:h3.form-name title]]])))

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
      (let [{:keys [title description downloadable id_string editing?]} form]
        (html
         (if editing?
           ;; Edit mode uses local-state to get+set inputs
           (let [{:keys [title description downloadable]} state]
             [:div.settings-form
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
                 "Inactive"]]]])
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
             [:div.pure-control-group
              [:label {:for "form-status"} "Status"]
              [:span {:class "detail-form-active"}
               (if downloadable "Active" "Inactive")]]]]))))))

(defmethod details-page :ona-zebra
  [{:keys [dataset-info]} owner]
  "Om component for the whole details page."
  (reify
    om/IRender
    (render [_]
      (html
       [:div#tab-contentsettings
        (om/build form-header dataset-info)
        [:div.container
         [:div.pure-g-r
          [:div.pure-u-3-4
           [:h3.t-red "Form"]]]
         (om/build settings-form dataset-info)]]))))
