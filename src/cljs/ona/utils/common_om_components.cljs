(ns ona.utils.common-om-components
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [dommy.macros :refer [sel sel1]])
  (:require [cljs.core.async :refer [<! chan put! close! mult tap timeout]]
            [clojure.string :as s]
            [dommy.core :as dommy]
            [om.core :as om :include-macros true]
            [ona.api.io :as io]
            [ona.utils.dom :as dom-utils]
            [ona.utils.shared-dom :as shared-dom]
            [ona.utils.string :refer [first-cap not-empty?]]
            [ona.utils.tags :refer [image]]
            [ona.utils.url :refer [last-url-param]]
            [sablono.core :refer-macros [html]]))

(defn main-menu
  "Component that renders main-menu"
  [{:keys [username forms dataset-info]} owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (dom-utils/init-dropdowns :.forms-search :.form-name))
    om/IRenderState
    (render-state [_ state]
      (let [username (if username
                       username
                       (om/get-shared owner [:username]))
            {:keys [title owner formid] } dataset-info
            form-owner (last-url-param owner)]
        (html [:div {:id (if forms
                           "main-menu"
                           "dataview-menu") :class "cfix"}
               [:div {:class "vw-menu pure-menu-horizontal container cfix"}
                [:a {:id "logo-link" :href "/" :class "pure-menu-heading left"}
                 [:span
                  [:img {:id "ona-logo" :src (image "onadata-logo.png") :alt "ONA"}]]]
                (if (and (not= username "null") (not-empty? username))
                  (if forms
                    [:ul {:class "left" :id "main-nav-links"}
                     [:li
                      [:a {:id "home-link" :href "/"}
                       [:span "Home"]]]
                     [:li {:id "forms"}
                      [:div {:class "dropdown projects-drop drop-hover"}
                       [:a {:href "/"}
                        [:span "Forms "
                         [:i {:class "fa fa-angle-down"}]]]
                       [:ul {:id "project-dropdown" :class "submenu"}
                        [:li [:div {:class "drop-search forms-search"}
                              [:i {:class"fa fa-search"}]
                              [:input {:type "text" :name "" :class "" :id "" :placeholder "Find a form"}]]]
                        (for [{:keys [formid title]} forms]
                          [:li [:a {:href (str "/forms/" formid)} [:span {:class "pager-icon-red num-datasets-pager"}]
                                [:span {:class "form-name"} title]]])]]]]
                    [:div {:class "pure-u-3-4 project-breadcrumb"}
                     [:a {:href "/" :class "tooltip" :id "user-label-link"}
                      [:span {:class "orange username label user"  :id "user-label"} (first-cap form-owner)]
                      [:span {:id "form-owner"} form-owner]]
                     [:i {:class "fa fa-play"}]
                     [:a {:title title  :href (str "/forms/" formid) :id "dataset-link"} title]])
                  [:ul {:class "left"}
                   [:li [:a {:class "btn-nav" :href "/join"} "Join Ona"]]])
                (if (and (not= username "null") (not-empty? username))
                  [:ul {:class "right" :id "user"}
                   [:li {:class "header-user"} [:span {:class "bell-wrap"} [:i {:class "fa fa-bell-o"}]]
                    [:div {:class "dropdown drop-hover"}
                     [:a {:id "user-avatar" :href "#"} [:span {:class "label label-initial orange"} (first-cap username)]
                      [:i {:class "fa fa-angle-down"}]]
                     [:ul {:class "submenu" :id "user-dropdown"}
                      [:li [:a {:href "/"} username]]
                      [:li [:a {:href "/logout"} "Logout"]]]]]]
                  [:ul {:class "right" :id "user"}
                   [:li
                    [:a {:href "/login"} "Sign In"]]])]])))))


;; MARKER HELPERS

(defn tooltip-link
  [{:keys [url tooltip text class]}]
  [:div {:class (str "inline-tip " class)}
   [:a {:target "_blank" :class "tooltip" :href url}
    [:span.tip-info tooltip] [:span.tip-question text]]])

;; OM COMPONENTS

(defn loading-spinner-component
  "A loading spinner as an om component. Attrs passed in via opts."
  [_ _ {:keys [attrs]}]
  (om/component (shared-dom/loading-spinner attrs)))

(defn modal-overlay
  "A faded black overlay for when modal is open. Should show below
  regular and confirmation modals. Only use in modal when clicking
  ovelay is supposed to close modal."
  []
  [:div {:class "modal-overlay"
         :on-click #(doseq [modal-container (sel :.modal-container)]
                     (dommy/hide! modal-container))}])

(defn confirm-modal
  "A confirmation dialog modal made with om.
   On 'confirm' / 'cancel', fires {:confirm true / false} event into
   confirmation-channel, which is expected to be passed in via opts.
   header text and btn-txt are also expected via opts."
  [_ _ {:keys [text header btn-text chan not-closable? no-confirm? no-cancel?]}]
  (reify
    om/IRender
    (render [_]
      (html
        [:div {:class "row modal-container overlay-dismiss"}
         (modal-overlay)
         [:div {:id "confirm-dialog" :class "modal modal-dialog modal-confirm"}
          [:div {:class "modal-wrap cfix"}
           [:div {:class "modal-header"} [:h2 header]
            (when-not not-closable?
              [:a {:href "#" :class "btn-close"
                   :on-click #(put! chan {:confirm false})} "×" ])]
           [:div {:class "modal-body"} text]
           [:div {:class "modal-footer"}
            (when-not no-confirm?
              [:a {:class "pure-button btn-warning t-bold" :href "#"
                   :on-click #(put! chan {:confirm true})} btn-text])
            (when-not no-cancel?
              [:a {:class "pure-button btn-default t-bold" :href "#"
                   :on-click #(put! chan {:confirm false})} "Cancel"])]]]]))))

(defn modal-spinner [_ _ opts]
  "A modal with a spinner in it. Om component, takes opts as confirm-modal does."
  (let [spn (shared-dom/loading-spinner {:class "t-2x"})
        new-opts (merge opts {:text spn         :not-closable? true
                              :no-confirm? true :no-cancel? true})]
    (om/component
      (om/build confirm-modal nil {:opts new-opts}))))

(defn- iframe-modal
  "A component to load an enketo iframe to either add or edit submission data.
   If the iframe re-directs, the modal will be closed.
   After modal is closed, on-finish (passed w/in opts) is called."
  [cursor owner {:keys [src on-finish remove-chan]}]
  (reify
    om/IInitState
    (init-state [_] {:closed false :id (gensym)})
    om/IRenderState
    (render-state [_ {:keys [closed id]}]
      (if closed
        (do (close! remove-chan) (on-finish) (html [:div]))
        (html
          [:div#preview-modal
           [:div.modal.modal-dialog
            [:a {:id "preview-modal-close" :class "btn-widget close right"
                 :href "#" :on-click #(om/set-state! owner :closed true)} "×"]
            [:iframe {:id id :src src
                      :width "700" :height "600" :scrolling "yes"
                      :marginheight "0" :vspace "0" :hspace "0"}]]])))
    om/IDidMount
    (did-mount [_]
      "In did mount, we close the modal when iframe re-directs.
       For this, we close the modal when `onload` is fired a second time."
      (when-let [iframe (dom-utils/by-id (om/get-state owner :id))]
        (aset iframe "onload"
              (fn [] (aset iframe "onload"
                           #(om/set-state! owner :closed true))))))))

(defn add-submission-button
  "An add submission button that gets enketo url as it mounts.
   Pass in role, auth-token, class, and optionally text via opts.
   link-cb is an optional callback, gets called when enketo link received."
  [dataset-id owner {:keys [auth-token role link-cb]}]
  (reify
    om/IInitState
    (init-state [_] {:data-entry-link nil})
    om/IWillMount
    (will-mount [_]
      (go (let [link (-> (io/make-url "forms" dataset-id "enketo.json")
                         (io/get-url {} auth-token)
                         <! :body :enketo_url)]
            (om/set-state! owner :data-entry-link link)
            (when link-cb (link-cb link)))))
    om/IRenderState
    (render-state [_ {:keys [data-entry-link]}]
      (html
        [:a {:target "_blank"
             :href (str "/webform?url="
                        (js/encodeURIComponent data-entry-link))}
         [:span {:class "icon-data submission"}]]))))

(defn- file-input [name filetype on-change]
  (let [i (.createElement js/document "input")]
    (aset i "type" "file")
    (aset i "name" name)
    (aset i "accept" filetype)
    (aset i "onchange" on-change)
    i))

(defn file-upload-button
  "A file upload button that has renders a button and a message alongside.
   Internally, component is staged into one of:
   :initial|:file-selected|:uploading|:error|:done
   :done is a stage for `single-upload?` buttons, transitioned to on success.
   `url` is the form POST url, `filetype`, `input-name` are file input attrs.
   `btn-text` goes on the upload button itself.
   `params` are mapped to hidden inputs so they are uploaded along with file.
   `pre-existing-filename` is displayed to the left of input in :initial stage.
   `messages` are a map from stage to dom, displayed after the upload input
    depending on stage.
    On :error stage, error-message from server is appended to message.
    On :done stage, filename uploaded is appended to message.
   `success-cb` is called upon successful upload."
  [_ owner {:keys [url filetype input-name btn-text params single-upload?
                   pre-existing-filename messages success-cb]}]
  (reify
    om/IInitState
    (init-state [_]
      (let [select-file! (fn [event]
                           (om/set-state! owner :file (.. event -target -value))
                           (om/set-state! owner :stage :file-selected))]
        {:stage :initial
         :input (file-input input-name filetype select-file!)
         :internal-channel (chan)}))
    om/IWillMount
    (will-mount [_]
      (go
        (while true
          (let [ch (om/get-state owner :internal-channel)
                {:keys [success? io-obj] :as e} (<! ch)]
            (try
              (let [json-response (-> (.getResponseJson io-obj)
                                      (js->clj :keywordize-keys true))
                    success-state (if single-upload? :done :initial)]
                (om/set-state! owner :stage (if success? success-state :error))
                (when (and success? success-cb) (success-cb json-response)))
              (catch js/Error e
                (om/set-state! owner :error-message (.getResponseText io-obj))
                (om/set-state! owner :stage :error)))))))
    om/IRenderState
    (render-state [_ {:keys [stage file internal-channel error-message]}]
      (let [deselect-file! (fn [event]
                             (om/set-state! owner :file nil)
                             (om/set-state! owner :stage :initial))
            upload! (fn [event]
                      (io/upload-file (om/get-node owner "upload-form")
                                      internal-channel)
                      (om/set-state! owner :stage :uploading)
                      (dommy/set-value! (om/get-state owner :input) ""))
            in-process? (contains? #{:file-selected :uploading} stage)
            upload-hidden? (or in-process? (= stage :done))
            fname #(-> % (s/split #"\\") last)]
        (html
          [:form {:action url :method "POST" :ref "upload-form"
                  :class "upload-form" :enc-type "multipart/form-data"}
           (for [[k v] params]
             [:input {:name k :type "hidden" :value v}])
           (when (and (= stage :initial) pre-existing-filename)
             [:span.ellipsis pre-existing-filename])
           [:div.btn-upload.pure-button
            {:ref "upload-button"
             :style (when upload-hidden? {:display "none"})} btn-text]
           (when in-process?
             [:span.uploaded-media
              [:span.ellipsis.filename (fname file)]
              (if (= stage :file-selected)
                [:span
                 [:i.fa.fa-times-circle.rm-file {:on-click deselect-file!}]
                 [:a.btn-upload.pure-button {:on-click upload!} "Upload"]]
                [:i.fa.fa-spin.fa-cog])])
           (case stage
             :error [:p.error (:error messages) [:br] error-message]
             :done [:span (get messages :done)
                    [:span.ellipsis.filename (fname file)]]
             (get messages stage))])))
    om/IDidMount
    (did-mount [_]
      (dommy/prepend! (om/get-node owner "upload-button")
                      (om/get-state owner :input)))
    om/IDidUpdate
    (did-update [_ _ state]
      (dommy/prepend! (om/get-node owner "upload-button")
                      (om/get-state owner :input)))))

;; HELPER FUNCTIONS to render OM components

(defn- component->container
  [cmp data args]
  (let [c (dom-utils/new-container!)]
    (om/root cmp data (merge {:target c} args))
    c))

(defn new-iframe-modal!
  "Creates a self-closing-modal given an om component, data, and args to render it.
   Om component must take :remove-chan in opts, which it closes to remove itself."
  [src on-finish]
  (let [remove-chan (chan 1)
        opts {:src src :on-finish on-finish :remove-chan remove-chan}
        container (component->container iframe-modal nil {:opts opts})]
    (go (<! remove-chan)
        (dommy/remove! container))))

(defn new-modal-spinner!
  "Creates a modal spinner, and add it to DOM. Delete modal when chan closed.
   Can specify a target, which will be removed from DOM at the end."
  ([chan] (new-modal-spinner! (dom-utils/new-container!) chan))
  ([chan target]
   (om/root modal-spinner nil {:target target})
   (go (do (<! chan)
           (dommy/remove! target)))))

(defn ask-for-confirmation!
  "Render a confirmation modal, and return a channel. On button click:
   (1) confirmation modal is removed from the dom.
   (2) {:corfirm true} or {:confirm false} is written onto returned channel.
   Argument opts is passed directly onto confirm-modal."
  [opts]
  (let [ch (chan 1)
        ch-mult (mult ch)
        ch-copy #(tap ch-mult (chan 1))
        new-opts (merge opts {:chan ch})
        container (component->container confirm-modal nil {:opts new-opts})]
    (go (<! (ch-copy)) (dommy/remove! container))
    (ch-copy)))

(defn xhr-confirm-modal
  "Confirmation modal for XHR requests, returns a channel.
   Behavior: Shows a confirm / cancel modal. On confirm, makes the xhr request.
             On xhr success, modal goes away, puts `true` on returned channel.
             On failure, display error confirm modal, put `nil` on channnel.
   Usage: (go (if (<! (xhr-confirm-modal ...)) success-action failure-action)."
  [opts xhr-action & [args]]
  (let [channel (ask-for-confirmation! opts)
        container (dom-utils/new-container!)]
    (go
      (when (:confirm (<! channel))
        (new-modal-spinner! (chan) container)
        (let [response (<! (apply xhr-action args))
              err-opts {:text (str "Something went wrong..."
                                   (or (:detail (:body response)) ""))
                        :no-confirm? true}]
          (dommy/remove! container)
          (if (:success response)
            true
            (do (ask-for-confirmation! err-opts) false)))))))

(defn download-link [url txt]
  [:a {:href url :download true :target "_blank" :class "d-ready"} txt])
