(ns ona.viewer.helpers.accounts
  (:require [milia.api.user :refer [user profile]]))

(defn get-account-credentials
  "Create an account map given a user's credentials."
  [username password]
  (let [account {:username username :password password}
        {:keys [username api_token temp_token]} (user account false true)]
    (when (and username api_token temp_token)
      {:username username
       :api_token api_token
       :temp_token temp_token})))


(defn build-session
  "Build session data for the account."
  [account]
  {:account (conj account (profile account))})
