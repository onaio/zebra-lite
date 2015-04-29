(ns ona.login
 (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                  [dommy.macros :refer [sel sel1]])
 (:require [cljs.core.async :refer [<! chan mult tap put!]]
           [om.core :as om :include-macros true]
           [sablono.core :refer-macros [html]]))

(def app-state (atom {}))

(defn login
  "Component that renders the login page and components."
  [cursor owner]
  (reify
    om/IRender
    (render [_]
      (html [:div
             [:div {:class "content container"}
              [:div {:class "standard-account-form"}
              [:h2 "Sign in"]
               [:form {:action "/login"
                       :method "post"
                       :class "pure-form pure-form-stacked ie-form"}
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
                 "Forgot your password?" [:a {:href "#" :class"link-red"} " Click here to reset it"]]]]]]))))

(defn ^:export init []
  (om/root login app-state
           {:target (sel1 :#app)}))