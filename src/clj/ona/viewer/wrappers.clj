(ns ona.viewer.wrappers
  (:require [clansi.core :as ansi]
            [clj-redis-session.core :refer [redis-store]]
            [clojure.set :refer [intersection]]
            [environ.core :refer [env]]
            [ona.utils.seq :refer [in?]]
            [ring.middleware.defaults :refer [site-defaults]]))

(def redactables
  #{:password
    :password1
    :password2
    :current_password})

(defn censor-credentials
  "Censor credentials that appear as values in a passed map."
  [uncensored]
  (merge uncensored
         (let [to-redact (intersection (-> uncensored keys set) redactables)]
           (zipmap to-redact (repeat (count to-redact) :REDACTED)))))

(defn safe-logger
  [{:keys [info debug] :as options}
   {:keys [request-method uri remote-addr query-string params] :as req}]
  (info (str (ansi/style "Starting " :cyan)
             request-method " "
             uri (when query-string (str "?" query-string))
             " for " remote-addr
             ;; log headers, but don't log username/password, if any
             " " (dissoc (:headers req) "authorization")))

  (debug (str "Request details: "
              (select-keys req [:server-port :server-name :remote-addr :uri
                                :query-string :scheme :request-method
                                :content-type :content-length :character-encoding])))
  (when params
    (info (str " \\ - - - - Params: " (censor-credentials params)))))

(def ona-site-defaults
  (-> site-defaults
      (assoc-in [:security :anti-forgery] false)
      (assoc-in [:session :store] (redis-store {:pool {} :spec {:host "127.0.0.1" :port 6379}}))))