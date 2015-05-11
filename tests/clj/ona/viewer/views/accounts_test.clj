(ns ona.viewer.views.accounts-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as string]
            [ona.viewer.views.accounts :refer :all]
            [ona.viewer.helpers.accounts :refer [get-account-credentials]]
            [ona.api.user :as user-api]
            [ona.api.http :refer [parse-http]]
            [ona.api.io :refer [make-url]]
            [ona.utils.remote :refer [protocol]]))


(def username "testusername")
(def account {:username username})
(def temp-token "a1b2c3")
(def invalid-token "Invalid token")
(def token-expired "Token expired")


(facts "About get-token token"
       (get-token account username) => (contains [[:body temp-token]]);
       (provided
         (user-api/user account) => {:temp_token temp-token}))

(facts "About validate invalid token"
       (validate-token account username) => {:status 403 :body invalid-token}
       (provided
         (user-api/user account true true) => {:detail invalid-token}))

(facts "About validate token expired"
       (validate-token account username) => {:status 403 :body token-expired}
       (provided
         (user-api/user account true true) => {:detail token-expired}))

;TODO Fix failing logout test
; (facts "about logout"
;       (fact "should set session to nil"
;             (:session (logout account false)) => nil
;             (provided
;              (user-api/expire-temp-token account) => nil)))

(facts "about login"
       (fact "should show message correct message when credentials are incorrect"
         (string/join (submit-login :username :password))
         => (contains "Invalid Username or Password")
         (provided
          (get-account-credentials :username :password) => nil)))
