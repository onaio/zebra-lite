(ns ona.dataset
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [dommy.macros :refer [sel sel1]])
  (:require [cljs.core.async :refer [<! chan mult tap put!]]
            [cljsjs.moment]
            [clojure.string :as s]
            [om.core :as om :include-macros true]
            [ona.api.io :as io]
            [ona.api.http :refer [parse-http]]
            [ona.state :refer [update-form-state! forms-state]]
            [ona.utils.common-om-components :refer [main-menu]]
            [ona.utils.dom :refer [click-fn]]
            [ona.utils.interop :refer [safe-regex]]
            [ona.utils.numeric :as n]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

(def menu "main-menu")
(def dataset-container "dataset-container")

(def sorters
  {"Date Created " {:func :date_created :comp >}
   "Alphabetical " {:func #(-> % :title s/lower-case) :comp <}
   "Last Submission " {:func #(if-not (empty? (:last_submission_time %))
                               (:last_submission_time %)
                               "1970-01-01") :comp >}
   "No. of Submissions " {:func :num_of_submissions :comp >}})

(defn show-inactive-forms
  [show app-state]
  (if show
    (om/update! app-state :active-forms (concat (:active-forms @forms-state)
                                                (remove :downloadable (:forms @forms-state))))
    (om/update! app-state :active-forms (filter :downloadable (:forms @forms-state)))))

(defn- no-forms-content []
  "Message to display when there are no forms"
  [:div {:class "empty-placeholders"}
   [:p {:id "no-forms"} "You don't have any forms!!" [:br]]])


(defn sort-forms [sorter owner event-chan]
  (om/set-state! owner :sorter sorter)
  (put! event-chan {:sort-list-by sorter}))

(defn filter-list
  "Takes a query string, and searches if any forms/projects in the view include
   that query, ignoring case, in either the title or the description."
  [query list]
  (if (s/blank? query)
    list
    (filter #(re-find (safe-regex query)
                      (str (:title %) (:description %) (:name %))) list)))

(defn handle-events [event-chan app-state]
  (go (while true
        (let [{:keys [sort-list-by filter-by
                      show-inactive]} (<! event-chan)]
          (when filter-by
            (let [forms (:forms @app-state)
                  form-list (if (:show-inactive @app-state)
                              forms
                              (filter :downloadable forms))]
              (om/update! app-state :active-forms
                          (filter-list filter-by form-list))))
          (when sort-list-by
            (let [{:keys [func comp]} (sorters sort-list-by)
                  sort-fn #(sort-by func comp %)]
              (om/transact! app-state :active-forms sort-fn)
              (om/transact! app-state :forms sort-fn)))
          (when show-inactive
            (let [show (:show show-inactive)]
              (om/update! app-state :show-inactive show)
              (show-inactive-forms show app-state)))))))


(defn form-list-header
  "The header for the form listing. Includes search / sort / add form elements"
  [cursor owner]
  (reify
    om/IInitState
    (init-state [_]
      {:sorter "Last Submission "})
    om/IWillMount
    (will-mount [_]
      (let [event-chan (om/get-shared owner [:event-chan])
            sorter (om/get-state owner [:sorter]) ]
        (sort-forms sorter
                    owner
                    event-chan)))
    om/IRenderState
    (render-state [_ state]
      (let [event-chan (om/get-shared owner [:event-chan])
            sorter (om/get-state owner [:sorter])]
        (html [:div {:class "cfix pure-g-r forms-filter"}
               [:div {:class "pure-u-1-5 page-header borderless"}
                [:h2 "Forms"]]

               [:div {:class "pure-u-1-2 sortbox"} "Sort by: "
                [:div {:class "dropdown drop-hover" :id "filter"}
                 [:span {:class "filter-criteria" :id "filter-criteria"} sorter]
                 [:i {:class "fa fa-angle-down"}]
                 [:ul {:class "submenu"}
                  (for [sorter (keys sorters)]
                    [:li [:a {:on-click (click-fn
                                          #(sort-forms sorter owner event-chan))
                              :href "#"} sorter]])]]
                [:span {:class "pure-u-1-3 show-inactive"}
                 [:input {:type "checkbox" :id "show-inactive"
                          :on-click #(put! event-chan {:show-inactive {:show (.. % -target -checked)}})}]
                 " Show inactive"]]

               [:div {:class "pure-u-1-3 form-search right"}
                [:form {:id "search-form"
                        :class "pure-form inline-block search-wrap right"}
                 [:input {:name "query"
                          :id "search-query"
                          :type "text"
                          :on-key-up #(put! event-chan
                                            {:filter-by (.. % -target -value)})}]
                 [:button {:class "pure-button" :type "submit"
                           :id "submit-search"}
                  [:i {:class "fa fa-search"}]]]]])))))

(defn form-item
  "An indivdual form on the project view."
  [{:keys [form]} owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (let [{:keys [formid title description downloadable metadata
                    num_of_submissions last_submission_time date_created]} form
            created_on (.format (js/moment date_created) "MMM DD, YYYY")
            ago-str (str "last " (.fromNow (js/moment last_submission_time)))
            form-url (io/make-url "forms" formid)]
        (html [:tr
               [:td
                [:a {:class "dataset-url" :href form-url}
                 (if downloadable
                   [:span {:class "pager-icon-red num-datasets-pager"}]
                   [:span {:class "pager-icon-grey num-datasets-pager"}])]]
               [:td
                (if downloadable
                  {:class "move-holder"}
                  {:class "move-holder inactive-form"})
                [:div {:class "pure-g-r project-row cfix form-info"}
                 [:div {:class "pure-u-1-3"}
                  [:strong
                   [:a {:class "dataset-name" :href form-url} title
                    (if-not downloadable " (inactive)")]]]

                 [:div {:class "pure-u-5-8 form-meta"}
                  [:span {:class "num-submissions"
                          :title (n/pluralize-number num_of_submissions "record")}
                   (n/shorten-number num_of_submissions)]
                  [:span {:class "date-created"}
                   [:i {:class "fa fa-clock-o"}]
                   [:span {:class "date-text"} created_on]]
                  [:span {:class "submissions"}
                   [:span {:class "sub-last-when"}
                    (if (pos? num_of_submissions)
                      [:span {:class "t-grey when"} ago-str]
                      [:span {:class "t-grey when"} " (no records)" ])]]]]]])))))

(defn datalist-page
  "Component for loading the datasets page"
  [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (handle-events (om/get-shared owner [:event-chan]) cursor))
    om/IRender
    (render [_]
      (let [{:keys [active-forms forms]} cursor]
        (html [:div
               (om/build form-list-header cursor)
               [:div {:class "forms-table"}
                 (cond
                   (empty? forms) (no-forms-content)
                   (empty? active-forms) [:div "Sorry, no results found!"]
                   :else [:table {:id "datasets-table"
                                  :class "pure-table pure-table-horizontal"}
                          [:tbody (for [form active-forms]
                                    (om/build form-item
                                              {:form form}))]])]])))))

(defn init-form-state [forms]
  (update-form-state! forms)
  forms-state)


(defn ^:export init [username auth-token]
  (om/root main-menu {:username username}
           {:target (. js/document (getElementById menu))})

  (go (let [valid-token (<! (io/validate-token auth-token username))
            forms-url (io/make-url "forms.json")
            ;; Find out why the login pop comes up
            forms-chan (io/get-url forms-url {} valid-token :no-cache? true)
            forms (-> (<! forms-chan) :body flatten)
            app-state (init-form-state forms)
            shared-state {:username username
                          :auth-token valid-token
                          :event-chan (chan)}]
        (om/root datalist-page app-state
                 {:target (. js/document (getElementById dataset-container))
                  :shared shared-state}))))

