(ns api.models.analysis
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-analysis)
(declare get-analysis)
(declare get-user-analysis)
(declare get-user-analysis)
(declare touch-analysis)
(declare delete-analysis)

(hugsql/def-db-fns "sql/analysis.sql")
(hugsql/def-sqlvec-fns "sql/analysis.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-analysis
  {:id                nil
   :of-type           nil
   :user-id           nil
   :readable-name     nil
   :created-on        nil
   :status            nil
   :progress          nil
   :viewer-url-params nil})

(defn upsert! [db {:keys [id status of-type] :as analysis}]
  (let [prev     (or (get-analysis db {:id id})
                     nil-analysis)
        analysis (cond-> (merge prev analysis)
                         status                   (update :status name)
                         of-type                  (update :of-type name))]
    (log/debug "upsert-analysis" analysis)
    (upsert-analysis db analysis)))
