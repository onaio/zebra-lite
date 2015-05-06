(ns ona.utils.common-om-components
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [dommy.macros :refer [sel sel1]])
  (:require [cljs.core.async :refer [<! chan put! close! mult tap timeout]]
            [om.core :as om :include-macros true]
            [ona.utils.tags :refer [image]]
            [ona.utils.string :refer [not-empty?]]
            [sablono.core :refer-macros [html]]))

(defn main-menu
  "Component that renders main-menu"
  [{:keys [username]} owner]
  (om/component
    (html [:div {:id "main-menu" :class "cfix"}
           [:div {:class "vw-menu pure-menu-horizontal container cfix"}
            [:a {:id "logo-link" :href "#" :class "pure-menu-heading left"}
             [:span
              [:img {:id "ona-logo" :src (image "onadata-logo.png") :alt "ONA"}]]]
            (if (and (not= username "null") (not-empty? username))
              [:ul {:class "left" :id "main-nav-links"}
               [:li
                [:a {:id "home-link" :href "#"}
                 [:span "Home"]]]
               [:li {:id "forms"}
                [:div {:class "dropdown projects-drop drop-hover"}
                 [:a {:href "#"}
                  [:span "Forms "
                   [:i {:class "fa fa-angle-down"}]]]
                 [:ul {:id "project-dropdown" :class "submenu"}
                  [:li [:div {:class "drop-search forms-search"}
                        [:i {:class"fa fa-search"}]
                        [:input {:type "text" :name "" :class "" :id "" :placeholder "Find a project"}]]]
                  [:li [:a {:href "#"} [:span {:class "label label-initial pink"}"A"] [:span {:class "form-name"} "Form 1"]]]
                  [:li [:a {:href "#"} [:span {:class "label label-initial pink"}"B"] [:span {:class "form-name"} "Form 2"]]]]]]]
              [:ul {:class "left"}
               [:li [:a {:class "btn-nav" :href "/join"} "Join Ona"]]])
            (if (and (not= username "null") (not-empty? username))
              [:ul {:class "right" :id "user"}
               [:li {:class "header-user"} [:span {:class "bell-wrap"} [:i {:class "fa fa-bell-o"}]]
                [:div {:class "dropdown drop-hover"}
                 [:a {:id "user-avatar" :href "#"} [:span {:class "label label-initial pink"} "G "]
                  [:i {:class "fa fa-angle-down"}]]
                 [:ul {:class "submenu" :id "user-dropdown"}
                  [:li [:a {:href "#"} username]]
                  [:li [:a {:href "#"} "Settings"]]
                  [:li [:a {:href "#"} "Help"]]
                  [:li [:a {:href "/logout"} "Logout"]]]]]]
              [:ul {:class "right" :id "user"}
               [:li
                [:a {:href "/login"} "Sign In"]]])]])))
