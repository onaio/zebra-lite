(ns ona.viewer.main
  (:require [ona.viewer.routes :as routes]
            [environ.core :refer [env]]
            [onelog.core :as log]
            [ring.adapter.jetty :as ring])
  (:gen-class))


(def app
  (do
    (log/start! (:stdout (env :std-out)))
    routes/ona-viewer))

(def app-production
  (do
    (log/start! (:file (env :file)))
    routes/ona-viewer))

(defn start [port]
  (ring/run-jetty app-production {:port port
                                  :join? false
                                  :min-threads (env :jetty-min-threads)
                                  :max-threads (env :jetty-max-threads)}))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") "8080"))]
    (start port)))
