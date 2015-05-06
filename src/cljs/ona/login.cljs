(ns ona.login
 (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                  [dommy.macros :refer [sel sel1]])
 (:require [cljs.core.async :refer [<! chan mult tap put!]]
           [cljs.reader :refer [read-string]]
           [om.core :as om :include-macros true]
           [ona.utils.common-om-components :refer [main-menu]]
           [ona.utils.dom :refer [click-fn]]
           [sablono.core :refer-macros [html]]
           [secretary.core :as secretary]))

(def app-state (atom {}))


(defn login
  "Component that renders the login page and components."
  [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [error-message]} cursor]
        (html [:div
               (om/build main-menu cursor)
               [:div {:class "content container"}
                [:div {:class "standard-account-form"}
                 [:h2 "Sign in"]
                 [:form {:action "/login"
                         :method "post"
                         :class "pure-form pure-form-stacked ie-form"}
                  (when error-message
                    [:p {:class "status-msg error"} error-message])
                  [:p
                   [:label "Username or Email"]
                   [:input {:type "text" :name "username" :placeholder "Username or Email" :autofocus true}]]
                  [:p
                   [:label "Password"]
                   [:input {:type "password" :name "password" :placeholder "Password"}]]
                  [:p
                   [:input {:type "submit" :value "Sign In" :class "pure-button btn-warning btn-large"}]]
                  [:p
                   "Don't have an account?" [:a {:href"/join" :class"link-red"} " Sign up"]]
                  [:p
                   "Forgot your password?" [:a {:href "#" :class"link-red"} " Click here to reset it"]]]]]])))))

(defn ^:export init [status-messages]

  (when (not= status-messages "null")
    (swap! app-state conj (read-string status-messages)))

  (om/root login app-state
           {:target (sel1 :#app)}))