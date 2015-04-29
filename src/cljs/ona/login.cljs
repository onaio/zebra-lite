(ns ona.login
 (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                  [dommy.macros :refer [sel sel1]])
 (:require [cljs.core.async :refer [<! chan mult tap put!]]
           [om.core :as om :include-macros true]
           [ona.utils.tags :refer [image]]
           [sablono.core :refer-macros [html]]))

(def app-state (atom {}))


(defn main-menu
  "Component that renders main-menu"
  [cursor owner]
  (om/component
    (html [:div {:id "main-menu" :class "cfix"}
           [:div {:class "vw-menu pure-menu-horizontal container cfix"}
           [:a {:id "logo-link" :href "#" :class "pure-menu-heading left"}
            [:span
             [:img {:id "ona-logo" :src (image "onadata-logo.png") :alt "ONA"}]]]
           [:ul {:class "left" :id "mian-nav-links"}
            [:li
             [:a {:id "home-link" :href "#"}
              [:span {:data-i18n-key "main-navigation/home-link-text"} "Home"]]]
            [:li
             [:div {:class "dropdown projects-drop drop-hover"}
              [:a {:href "#"}
               [:span {:data-i18n-key "main-navigation/organizations-dropdown-header"} "Organizations"
                [:i {:class "fa fa-angle-down"}]]]
              [:ul {:id "orgs-dropdown" :class "submenu"}
               [:li [:div {:class "drop-search orgs-search"}
                     [:i {:class"fa fa-search"}]
                     [:input {:type "text" :name "" :class "" :id "" :placeholder "Find an organization"}]]]
               [:li [:a {:href "#"} [:span {:class "label label-initial pink"}"A"] [:span {:class "org-name"} "Org 1"]]]
               [:li [:a {:href "#"} [:span {:class "label label-initial pink"}"B"] [:span {:class "org-name"} "Org 2"]]]]]]
            [:li {:id "projects"}
             [:div {:class "dropdown projects-drop drop-hover"}
              [:a {:href "#"}
               [:span {:data-i18n-key "main-navigation/main-navigation/projects-dropdown-header"} "Projects"
                [:i {:class "fa fa-angle-down"}]]]
              [:ul {:id "project-dropdown" :class "submenu"}
               [:li [:div {:class "drop-search orgs-search"}
                     [:i {:class"fa fa-search"}]
                     [:input {:type "text" :name "" :class "" :id "" :placeholder "Find a project"}]]]
               [:li [:a {:href "#"} [:span {:class "label label-initial pink"}"A"] [:span {:class "proj-name"} "Project 1"]]]
               [:li [:a {:href "#"} [:span {:class "label label-initial pink"}"B"] [:span {:class "proj-name"} "Project 2"]]]]]]
            [:li [:a {:id "whatsnew-link" :data-i18n-key "main-navigation/new-features-link-text" :href "#"} "What's New"]]]
           [:ul {:id "user" :class "right"}
            [:li {:class "header-user"} [:span {:class "bell-wrap"} [:i {:class "fa fa-bell-o"}]]
             [:div {:class "dropdown drop-hover"}
              [:a {:id "user-avatar" :href "#"} [:span {:class "label label-initial pink"} "G"]
               [:i {:class "fa fa-angle-down"}]]
              [:ul {:class "submenu" :id "user-dropdown"}
               [:li [:a {:href "#"} "username"]]
               [:li [:a {:href "#"} "Settings"]]
               [:li [:a {:href "#"} "Help"]]
               [:li [:a {:href "#"} "Logout"]]]]]]]])))

(defn login
  "Component that renders the login page and components."
  [cursor owner]
  (reify
    om/IRender
    (render [_]
      (html [:div
             (om/build main-menu cursor)
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