(ns ona.viewer.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ona.viewer.views.accounts :as accounts]
            [ona.viewer.views.datasets :as datasets]
            [ona.viewer.views.home :as home]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [slingshot.slingshot :refer [throw+ try+]]))

(defroutes site-routes
  (GET "/about" [] (home/about-page)))

#_(defroutes user-routes
  (GET "/login" {session :session flash :flash} (accounts/login session flash))
  (POST "/login"
        {{:keys [username password]} :params}
        (accounts/submit-login username password))
  (GET "/logout"
       {{:keys [account]} :session}
       (accounts/logout account)))

#_(defroutes dataset-routes
  (context "/:owner" [owner]
           (GET "/forms"
                {{:keys [account]} :session
                 {xhr? :xhr} :params}
                (datasets/list account owner project-id xhr?))
           (context "/:dataset-id" [dataset-id]
                    (GET "/"
                         {{:keys [account]} :session}
                         (datasets/show account
                                        owner
                                        dataset-id)))))

(defroutes main-routes
  (GET "/"
       {{:keys [account]} :session
        params :params}
       (home/home-page account))
  (route/resources "/")
  (route/not-found "Page not found"))

(defroutes app-routes
  site-routes
  ; user-routes
  ; dataset-routes
  main-routes)

#_(defn wrap-error-handler
  "Globally catch errors."
  [handler]
  (fn [{{:keys [account]} :session :as request}]
    (try+
      (handler request)
      (catch #(in? [401 404] (:api-response-status %)) error-response
        (if @debug?
          (throw+)
          (response-with-status
           (errors/not-found account) 404)))
      (catch Object _
        (if @debug?
          (throw+)
          (do
            (send-error-alert request &throw-context)
            (response-with-status (errors/error account) 500)))))))

#_(defn safe-logger
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

(def ona-viewer
  (-> (routes app-routes)
      ;; catch 500 and 40x throwables and return an error or not found page.
      #_(wrap-error-handler)
      ;; a variety of default site handler, see ring-defaults docs.
      (wrap-defaults ona-site-defaults)
      ;; log after we have processed the request to its final state.
      #_(wrap-with-logger :pre-logger safe-logger)
      ;; check GZIP after we have added the content types.
      (wrap-gzip)))
