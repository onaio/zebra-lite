(ns ona.utils.shared-dom
  (:require [clojure.string :as s]
            #+cljs [sablono.core :as html :refer-macros [html]]
            #+clj  [hiccup.page :refer [html5]]))

(defn loading-spinner
  "A loading spinner. Additional attributes for encompassing div is first arg.
   An optional second arg ('text') is included after the spinner div."
  ([] (loading-spinner nil nil))
  ([attrs] (loading-spinner attrs nil))
  ([attrs text]
   (let [space-join (fn [& args] (s/join " " args))
         div-attrs (merge-with space-join attrs {:class "t-center"})]
     (#+cljs html
      #+clj html5
      [:div div-attrs
       [:div {:class "sk-spinner sk-spinner-wave"}
        (for [i (range 1 6)] [:div {:class (str "sk-rect" i)}])]
       [:div.caption text]]))))
