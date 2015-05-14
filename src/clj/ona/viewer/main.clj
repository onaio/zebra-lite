(ns ona.viewer.main
  (:require [ona.viewer.routes :as routes]
            [environ.core :refer [env]]
            [onelog.core :as log]
            [milia.utils.remote :refer [hosts]]
            [ring.adapter.jetty :as ring])
  (:gen-class))


(def app
  (do
    (swap! hosts assoc :data (env :ona-api-server-host))
    (swap! hosts assoc :ona-api-server-protocol (env :ona-api-server-protocol))
    (log/start! (env :stdout))
    routes/ona-viewer))

(def app-production
  (do
    (swap! hosts assoc :data (env :ona-api-server-host))
    (swap! hosts assoc :ona-api-server-protocol (env :ona-api-server-protocol))
    (log/start! (env :file))
    routes/ona-viewer))

(defn start [port]
  (ring/run-jetty app-production {:port port
                                  :join? false
                                  :min-threads (env :jetty-min-threads)
                                  :max-threads (env :jetty-max-threads)}))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") "8080"))]
    (start port)))
