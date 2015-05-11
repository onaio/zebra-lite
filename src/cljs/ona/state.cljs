(ns ona.state
  (:require [om.core :as om :include-macros true]))

;; State objects of Zebra Light

(def forms-state
  (atom {:forms []
         :active-forms []
         :username []}))

;; State manipulation utilities

(defn transact!
  [app-state]
  (if (satisfies? om/ITransact app-state)
    om/transact! swap!))

(defn transact-app-state!
  [app-state ks transact-fn]
  ((transact! app-state) app-state update-in ks transact-fn))

(defn transact-app-data!
  "Given a function over data, run a transact on data inside app-state."
  [transact-fn]
  ((transact! forms-state) forms-state update-in [:forms] transact-fn))

(defn update-form-state!
  "Given `data` received from the server, update the app-state."
  [data]
  (transact-app-data! (fn [_] data))
  (transact-app-state! forms-state [:active-forms] (fn [_] (remove #(= (:downloadable %) false) data))))