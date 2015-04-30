(ns ona.dataset-tests
  (:require-macros [cljs.test :refer (is deftest testing)]
                   [dommy.macros :refer [node sel sel1]])
  (:require [cljs.core.async :refer [<! chan put!]]
            [dommy.core :as dommy]
            [om.core :as om :include-macros true]
            [ona.dataset :as dataset]
            [ona.state :as state]
            [ona.utils.dom :as dom-utils]
            [ona.utils.interop :refer [format]]))

(def username "username")
(def auth-token "auth-token")
(def event-chan (chan))

;; SAMPLE DATA

(defn forms-list-gen [n]
  (let [rf (fn [max] (format "%02d" (inc (rand-int max))))]
    (for [i (range n)]
      {:formid i
       :title (str "Form " i)
       :description (str "Form description " i)
       :num_of_submissions i
       :downloadable (even? i)
       :date_created (str "2012-" (rf 12) "-" (rf 30))
       :metadata [{:data_type "enketo_url" :data_value "ENKETO_URL"}
                  {:data_type "enketo_preview_url" :data_value "ENKETO_PREVIEW_URL"}]
       :last_submission_time (str "2012-" (rf 12) "-" (rf 30))})))

(defn- form-list-container
  [forms]
  "Returns a container in which a form list has been rendered."
  (let [c (dom-utils/new-container!)
        data-atom (dataset/init-form-state forms)
        shared {:username username
                :auth-token auth-token
                :event-chan event-chan}]
    (om/root dataset/datalist-page data-atom {:target c :shared shared})
    c))

;; TESTS

(deftest empty-message-display-on-no-forms
   (testing "no forms message displays"
      (is (re-find #"You don't have any forms"
                   (dommy/text (form-list-container []))))))

(deftest forms-list-renders-properly
   (let [myforms (remove #(= (:downloadable %) false) (forms-list-gen 5))
         cont (form-list-container myforms)
         forms-table (sel1 cont :.forms-table)]
     (testing "forms list renders all forms"
        (is (= (count myforms) (count (sel cont :tr))))
        (is (= (map :title myforms)
               (map dommy/text (sel forms-table :a.dataset-name))))
        (is (= (map :downloadable myforms)
               (map #(dommy/has-class? % "pager-icon-red")
                    (sel forms-table :span.num-datasets-pager))))
        (is (= (map #(not (:downloadable %)) myforms)
               (map #(dommy/has-class? % "pager-icon-grey")
                    (sel forms-table :span.num-datasets-pager))))
        (is (= (count (sel :tr forms-table))
               (count (sel :li.replace-active forms-table))))
        (is (every? #{true}
                    (map (fn [form el]
                           (if (re-find #"no records" (dommy/text el))
                             (zero? (:num_of_submissions form))
                             (pos? (:num_of_submissions form))))
                         myforms (sel forms-table :span.when)))))

     (testing "Last submission is default sorter"
        (is (= (dommy/text (sel1 cont :.filter-criteria))) "Last Submission "))))
