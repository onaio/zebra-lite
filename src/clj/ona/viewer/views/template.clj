(ns ona.viewer.views.template
  (:require [hiccup.page :refer [html5]]
            [ona.utils.tags :as tags]))

(def default-css
  (map #(str "/css/" %)
       ["normalize.css"
        "font-awesome.min.css"
        "pure-min.css"
        "style.css"
        "font-awesome.min.css"
        "proxima-nova.css"]))

(defn base
  [title content & js]
  (html5
  [:head
   [:title title]
   (for [style-sheet default-css]
     [:link {:rel "stylesheet" :type "text/css" :href style-sheet}])]
  [:link {:rel  "stylesheet"
          :href "//cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.css"}]
  [:link {:rel  "stylesheet" :href "/css/slick.grid.css"}]
  [:link {:rel  "stylesheet" :href "/css/slick-default-theme.css"}]
  [:body
   [:content {:class "wrapper cfix"}
    [:div {:id "app"}
     content]]
   (tags/include-js "lib/main.js")
   (for [script js] script)]))
