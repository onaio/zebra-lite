(ns ona.dataview.shared
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [dommy.macros :refer [sel1]])
  (:require [cljs.core.async :refer [<! chan mult tap put! timeout]]
            [om.core :as om :include-macros true]
            [hatti.shared :refer [app-state event-chan] :as hatti-shared]
            [ona.api.io :as io]
            [ona.api.async-export :as export-api]
            [ona.utils.interop :refer [json->cljs]]))

(def default-fields
  [{:full-name "_submission_time" :label "Submission Time"
    :name "_submission_time" :type "datetime"}])

(defn update-data-on!
  "Updates data following CRUD events.
   On :add, just pull a new version of the data.
   On :edit, modify record of given :instance-id.
   On :delete, delete record of given :instance-id."
  [event {:keys [instance-id dataset-id auth-token] :as info}]
  (let [old-data (get-in @app-state [:map-page :data])
        this-record? #(= instance-id (get % "_id"))]
    (case event
      :add (go
            (let [url (io/make-url "data" (str dataset-id ".json"))
                  new-data (-> (io/raw-get-url url {} auth-token :no-cache? true)
                               <! :body json->cljs)]
              (hatti-shared/update-app-data! app-state new-data :rerank? true)))
      :delete (let [new-data (remove this-record? old-data)]
                (hatti-shared/update-app-data! app-state new-data :rerank? true))
      :edit (go
             (let [url (io/make-url "data" dataset-id (str instance-id ".json"))
                   new-record (-> (io/raw-get-url url {} auth-token :no-cache? true)
                               <! :body json->cljs)
                   rank (get (first (filter this-record? old-data)) "_rank")
                   new-record (merge new-record
                                     {"_id" instance-id "_rank" rank})
                   updater (partial map #(if (this-record? %) new-record %))]
               (hatti-shared/transact-app-data! app-state updater))))
    (put! event-chan {:data-updated true})))

;; SHARED HELPERS

(defn watch-async-export!
  "Helper function for download menu. Triggers an async export and
   stores the job-uuid in the local state of the owner.";
  ([dataset-id fmt owner]
   (watch-async-export! dataset-id fmt owner nil))
  ([dataset-id fmt owner opts]
   (let [{:keys [auth-token]} (om/get-shared owner)
        set-state-and-fmt! #(om/set-state! owner {%1 %2 :fmt fmt})]
    (export-api/trigger-async-export! auth-token
                                      dataset-id
                                      fmt
                                      #(set-state-and-fmt! :job_uuid %)
                                      #(set-state-and-fmt! :export_url %)
                                      opts))))
