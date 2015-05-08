(ns ona.viewer.views.template
  (:require [hiccup.page :refer [html5]]
            [ona.utils.tags :as tags]))

(def style-sheets
  (map #(str "/css/" %)
       ["normalize.css"
        "font-awesome.min.css"
        "pure-min.css"
        "style.css"
        "font-awesome.min.css"
        "proxima-nova.css"
        "slick.grid.css"
        "slick-default-theme.css"]))

(defn base
  [title content & js]
  (html5
  [:head
   [:title title]
   (for [style-sheet style-sheets]
     [:link {:rel "stylesheet" :type "text/css" :href style-sheet}])]
  [:body
   [:content {:class "wrapper cfix"}
    [:div {:id "app"}
     content]]
   (tags/include-js "lib/main.js")
   (for [script js] script)]))
