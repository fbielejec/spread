(ns api.models.time-slicer
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-time-slicer)
(declare get-time-slicer)
(declare update-time-slicer)
(declare insert-attribute)
(declare get-attributes)
(declare upsert-status)
(declare get-status)

(hugsql/def-db-fns "sql/time_slicer.sql")
(hugsql/def-sqlvec-fns "sql/time_slicer.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-time-slicer
  {:id                                      nil
   :user-id                                 nil
   :trees-file-url                          nil
   :slice-heights-file-url                  nil
   :readable-name                           nil
   :burn-in                                 nil
   :number-of-intervals                     nil
   :relaxed-random-walk-rate-attribute-name nil
   :trait-attribute-name                    nil
   :hpd-level                               nil
   :contouring-grid-size                    nil
   :timescale-multiplier                    nil
   :most-recent-sampling-date               nil
   :output-file-url                         nil})

(defn upsert! [db time-slicer]
  (let [time-slicer (->> time-slicer
                         (merge nil-time-slicer))]
    (log/debug "upsert-time-slicer!" time-slicer)
    (upsert-time-slicer db time-slicer)))

(defn update! [db time-slicer]
  (let [time-slicer (->> time-slicer
                         (merge nil-time-slicer))]
    (log/debug "update-time-slicer!" time-slicer)
    (update-time-slicer db time-slicer)))

(defn insert-attributes! [db time-slicer-id attributes]
  (doseq [att attributes]
    (insert-attribute db {:time-slicer-id time-slicer-id :attribute-name att})))

(defn upsert-status! [db status]
  (let [status (->> status
                    (merge {:time-slicer-id nil :status nil :progress nil})
                    (#(update % :status name)))]
    (log/debug "upsert-status!" status)
    (upsert-status db status)))
