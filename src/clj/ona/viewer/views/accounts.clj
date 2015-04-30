(ns ona.viewer.views.accounts
  (:require [ona.api.user :as user]
            [ona.utils.shared-dom :refer [loading-spinner]]
            [ona.utils.tags :as tags]
            [ona.viewer.helpers.accounts :refer [build-session get-account-credentials]]
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