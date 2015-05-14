(ns ona.viewer.views.accounts
  (:require [milia.api.user :as user]
            [ona.utils.shared-dom :refer [loading-spinner]]
            [ona.utils.tags :as tags]
            [ona.viewer.helpers.accounts :refer [build-session get-account-credentials]]
            [milia.api.user :as user-api]
            [ona.viewer.views.template :as template]
            [ring.util.response :as response]))

(defn login
  "Render the login page."
  ([session flash]
   (if (:account session)
     (response/redirect "/")
     (let [status-messages (:status-messages flash)]
       (template/base "Login"
                      (loading-spinner {:class "fullpage-spinner"} "Loading Zebra...")
                      (tags/js-tag (format "ona.login.init('%s')" status-messages)))))))
(defn submit-login
  "Process submitted login details and log the user in."
  [username password]
  (if-let [account (get-account-credentials username password)]
    (assoc (response/redirect "/")
      :session (build-session account))
    (assoc (response/redirect "/login")
      :flash {:status-messages {:error-message "Invalid Username or Password"}})))

(defn logout
  "Logout the user by empying the session."
  ([account] (logout account true))
  ([account background?]
   (let [expire-fn '(user/expire-temp-token account)]
     (if background? (future (eval expire-fn)) (eval expire-fn))
     {:status 302
      :headers {"Location" "/"}
      :body ""
      :session nil})))

(def not-allowed-response {:status 401 :body "Not Allowed"})

(defn get-token
  "Get auth token for logged in user"
  [account owner]
  (if (= (:username account) owner)
    (when-let [user (user-api/user account)]
      (let [temp_token (:temp_token user)
            account (assoc account :temp_token temp_token)]
        (assoc (response/response (:temp_token user)) :session {:account account})))
    not-allowed-response))

(defn validate-token
  [account owner]
  (if (= (:username account) owner)
    (let [user (user-api/user account true true)
          invalid-token "Invalid token"
          token-expired "Token expired"
          response (fn [msg]
                     {:status 403 :body msg})]
      (cond
        (= user {:detail invalid-token}) (response invalid-token)
        (= user {:detail token-expired}) (response token-expired)
        :else (:temp_token account)))
    not-allowed-response))

