(ns ona.viewer.routes_tests
  (:require [midje.sweet :refer :all]
            [ona.viewer.routes :refer :all]
            [ona.viewer.views.accounts :as accounts]))

(def username "username")
(def session {:account :fake-account})
(def result {:body :something})

(defn- route-params
  "Make route params"
  ([method uri]
   {:request-method method :uri uri})
  ([method uri session]
   (route-params method uri session nil))
  ([method uri session params]
   (assoc (route-params method uri) :session session :params params)))

(facts "user-routes"
       (fact "GET /:owner/temp-token should calls get-token"
             (user-routes (route-params :get
                                        (str "/" username "/temp-token")
                                        session)) => (contains result)
             (provided
               (accounts/get-token :fake-account username) => result))

       (fact "GET :owner/validate-token calls validate-token"
             (user-routes (route-params :get
                                        (str "/" username "/validate-token")
                                        session)) => (contains result)
             (provided
               (accounts/validate-token :fake-account username) => result))

       (fact "GET /login calls login"
             (user-routes (assoc (route-params :get
                                        "/login"
                                        session)
                            :flash :flash-value)) => (contains result)
             (provided
               (accounts/login session :flash-value) => result))

       (fact "POST /login calls login"
             (user-routes (route-params :post
                                        "/login"
                                        session
                                        {:username :username
                                         :password :password})) => (contains result)
             (provided
               (accounts/submit-login :username :password) => result))

       (fact "GET /logout calls logout"
             (user-routes (route-params :get
                                        "/logout"
                                        session)) => (contains result)
             (provided
               (accounts/logout :fake-account) => result)))
