(ns api.resolvers
  (:require [api.db :as db]
            [taoensso.timbre :as log]))

;; TODO: query RDS
(defn continuous-tree->attributes
  [{:keys [db]} _ {tree-id :treeId}]

  (log/info "continuous-tree->attributes" {:tree/id tree-id})

  ["att1" "att2"]

  )

;; TODO : read status (from RDS)
(defn get-parser-execution
  [_ {:keys [id db] :as args} _]
  (log/info "get_parser_execution" {:a args})
  {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :status :SUCCEEDED
   :output "s3://spread-dev-uploads/4d07edcf-4b4b-4190-8cea-38daece8d4aa"})
