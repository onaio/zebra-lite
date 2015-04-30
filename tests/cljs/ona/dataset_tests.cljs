(ns ona.dataset-tests
  (:require-macros [cljs.test :refer (is deftest testing)])
  (:require [cljs.core.async :refer [<! chan put!]]
            [dommy.core :as dommy]
            [om.core :as om :include-macros true]
            [ona.dataset :as dataset]
            [ona.state :as state]
            [ona.utils.dom :as dom-utils]))

(def username "username")
(def auth-token "auth-token")
(def event-chan (chan))

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
