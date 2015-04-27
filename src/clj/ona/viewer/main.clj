(ns ona.viewer.main
  (:require [ona.viewer.routes :as routes]
            [environ.core :refer [env]]
            [onelog.core :as log]
            [ring.adapter.jetty :as ring])
  (:gen-class))


(def app
  (do
    (log/start! "/dev/stdout")
    routes/ona-viewer))

(def app-production
  (do
    (log/start! "/var/log/ona-viewer/current")
    routes/ona-viewer))

(defn start [port]
  (ring/run-jetty app-production {:port port
                                  :join? false
                                  :min-threads 10
                                  :max-threads 80}))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") "8080"))]
    (start port)))
