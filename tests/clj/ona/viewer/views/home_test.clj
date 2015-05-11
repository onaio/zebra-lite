(ns ona.viewer.views.home-test
  (:require [midje.sweet :refer :all]
            [ona.viewer.views.accounts :as accounts]
            [ona.viewer.views.datasets :as datasets]
            [ona.viewer.views.home :refer :all]))

(def username "testusername")
(def account {:username username})
(def result {:body :something})

(facts "about dashboard"
       (fact "show datasets when session exists"
             (dashboard account) => (contains result)
             (provided
               (datasets/all account) => result))

       (fact "show login when session empty"
             (dashboard {}) => (contains result)
             (provided
               (accounts/login {} nil) => result)))
