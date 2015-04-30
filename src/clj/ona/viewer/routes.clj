(ns ona.viewer.routes
  (:require [clojure.set :refer [intersection]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ona.viewer.views.accounts :as accounts]
            [ona.viewer.views.datasets :as datasets]
            [ona.viewer.views.defaults :as defaults]
            [ona.viewer.views.home :as home]
            [ona.viewer.wrappers :as wrappers]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [slingshot.slingshot :refer [throw+ try+]]))

(defroutes site-routes
  (GET "/about" [] (home/about-page)))

(defroutes user-routes
  (GET "/login" {session :session flash :flash} (accounts/login session flash))
  (POST "/login" {{:keys [username password]} :params} (accounts/submit-login username password))
  (GET "/logout" {{:keys [account]} :session} (accounts/logout account))
  (context "/:owner" [owner]
    (GET "/temp-token"
         {{:keys [account]} :session}
      (accounts/get-token account owner))
    (GET "/validate-token"
         {{:keys [account]} :session}
         (accounts/validate-token account owner))))

#_(defroutes dataset-routes
  (context "/:owner" [owner]
           (context "/:dataset-id" [dataset-id]
                    (GET "/"
                         {{:keys [account]} :session}
                         (datasets/show account
                                        owner
                                        dataset-id)))))

(defroutes main-routes
  (GET "/" {{:keys [account]} :session} (home/dashboard account))
  (GET "/about" [] (defaults/about-page))
  (route/resources "/")
  (route/not-found "Page not found"))

(defroutes app-routes
  site-routes
  user-routes
  ; dataset-routes
  main-routes)

(def ona-viewer
  (-> (routes app-routes)
      ;; a variety of default site handler, see ring-defaults docs.
      (wrap-defaults wrappers/ona-site-defaults)
      ;; log after we have processed the request to its final state.
      (wrap-with-logger :pre-logger wrappers/safe-logger)
      ;; check GZIP after we have added the content types.
      (wrap-gzip)))
