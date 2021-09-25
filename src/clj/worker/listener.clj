(ns worker.listener
  (:require [api.db :as db]
            [api.models.analysis :as analysis-model]
            [api.models.bayes-factor :as bayes-factor-model]
            [api.models.continuous-tree :as continuous-tree-model]
            [api.models.discrete-tree :as discrete-tree-model]
            [api.models.time-slicer :as time-slicer-model]
            [aws.s3 :as aws-s3]
            [aws.sqs :as aws-sqs]
            [aws.utils :refer [s3-url->id]]
            [clojure.core.match :refer [match]]
            [clojure.data.json :as json]
            [mount.core :as mount :refer [defstate]]
            [shared.errors :as errors]
            [shared.utils :refer [file-exists? longest-common-substring round decode-json
                                  tree-object-extension
                                  trees-object-extension
                                  locations-object-extension
                                  log-object-extension
                                  output-object-extension]]
            [taoensso.timbre :as log]
            [worker.maps :as maps])
  (:import [com.spread.parsers BayesFactorParser ContinuousTreeParser DiscreteTreeParser TimeSlicerParser]
           com.spread.progress.IProgressObserver))

(declare listener)
(declare parse-time-slicer)

(defonce tmp-dir "/tmp")

(defn new-progress-handler [callback]
  (proxy [IProgressObserver] []
    (handleProgress [progress]
      (callback progress))))

(defmulti handler
  (fn [{:message/keys [type]} _]
    type))

(defmethod handler :default
  [{:message/keys [type]} _]
  (log/warn (str "No handler for message type " type)))

(defmethod handler :discrete-tree-upload
  [{:keys [id user-id] :as args} {:keys [db s3 bucket-name]}]
  (log/info "handling discrete-tree-upload" args)
  (try
    (let [;; TODO: parse extension
          tree-object-key (str user-id "/" id tree-object-extension)
          tree-file-path  (str tmp-dir "/" tree-object-key)
          _               (aws-s3/download-file s3 {:bucket    bucket-name
                                                    :key       tree-object-key
                                                    :dest-path tree-file-path})
          parser          (doto (new DiscreteTreeParser)
                            (.setTreeFilePath tree-file-path))
          attributes      (json/read-str (.parseAttributes parser))]
      (log/info "Parsed attributes" {:id         id
                                     :attributes attributes})
      (discrete-tree-model/insert-attributes! db id attributes)
      (analysis-model/upsert! db {:id     id
                                  :status :ATTRIBUTES_PARSED}))
    (catch Exception e
      (log/error "Exception when handling discrete-tree-upload" {:error e})
      (errors/handle-analysis-error! db id e))))

(defmethod handler :parse-discrete-tree
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config maps-boxes]}]
  (log/info "handling parse-discrete-tree" args)
  (try
    (let [{:keys [user-id locations-attribute-name
                  timescale-multiplier most-recent-sampling-date
                  locations-file-url]}
          (discrete-tree-model/get-tree db {:id id})
          ;; TODO: parse extension
          tree-object-key      (str user-id "/" id tree-object-extension)
          tree-file-path       (str tmp-dir "/" tree-object-key)
          ;; is it cached on disk?
          _                    (when-not (file-exists? tree-file-path)
                                 (aws-s3/download-file s3 {:bucket    bucket-name
                                                           :key       tree-object-key
                                                           :dest-path tree-file-path}))
          locations-file-id    (s3-url->id locations-file-url user-id)
          ;; TODO: parse extension
          locations-object-key (str user-id "/" locations-file-id locations-object-extension)
          locations-file-path  (str tmp-dir "/" locations-object-key)
          ;; is it cached on disk?
          _                    (when-not (file-exists? locations-file-path)
                                 (aws-s3/download-file s3 {:bucket    bucket-name
                                                           :key       locations-object-key
                                                           :dest-path locations-file-path}))
          progress-handler     (new-progress-handler (fn [progress]
                                                       (let [progress (round 2 progress)]
                                                         (when (= (mod progress 0.1) 0.0)
                                                           (log/debug "discrete tree progress" {:id       id
                                                                                                :progress progress})
                                                           (analysis-model/upsert! db {:id       id
                                                                                       :status   :RUNNING
                                                                                       :progress progress})))))
          parser               (doto (new DiscreteTreeParser)
                                 (.setTreeFilePath tree-file-path)
                                 (.setLocationsFilePath locations-file-path)
                                 (.setLocationTraitAttributeName locations-attribute-name)
                                 (.setTimescaleMultiplier timescale-multiplier)
                                 (.setMostRecentSamplingDate most-recent-sampling-date)
                                 (.registerProgressObserver progress-handler))
          output-object-key    (str user-id "/" id output-object-extension)
          output-object-path   (str tmp-dir "/" output-object-key)
          output-json-str      (.parse parser)
          _                    (spit output-object-path output-json-str :append false)
          _                    (aws-s3/upload-file s3 {:bucket    bucket-name
                                                       :key       output-object-key
                                                       :file-path output-object-path})
          url                  (aws-s3/build-url aws-config bucket-name output-object-key)]
      ;; TODO : in a transaction
      (discrete-tree-model/upsert! db {:id              id
                                       :output-file-url url})

      (analysis-model/upsert! db {:id     id
                                  :status :SUCCEEDED
                                  :viewer-url-params (maps/build-viewer-url-params user-id id maps-boxes (decode-json output-json-str))}))
    (catch Exception e
      (log/error "Exception when handling parse-discrete-tree" {:error e})
      (errors/handle-analysis-error! db id e))))

(defmethod handler :continuous-tree-upload
  [{:keys [id user-id] :as args} {:keys [db s3 bucket-name]}]
  (log/info "handling continuous-tree-upload" args)
  (try
    (let [;; TODO: parse extension
          tree-object-key (str user-id "/" id tree-object-extension)
          tree-file-path  (str tmp-dir "/" tree-object-key)
          _               (aws-s3/download-file s3 {:bucket    bucket-name
                                                    :key       tree-object-key
                                                    :dest-path tree-file-path})
          parser          (doto (new ContinuousTreeParser)
                            (.setTreeFilePath tree-file-path))
          attributes      (json/read-str (.parseAttributes parser))]
      (log/info "Parsed attributes and hpd-levels" {:id         id
                                                    :attributes attributes})
      ;; TODO : in a transaction
      (continuous-tree-model/insert-attributes! db id attributes)
      (analysis-model/upsert! db {:id     id
                                  :status :ATTRIBUTES_PARSED}))
    (catch Exception e
      (log/error "Exception when handling continuous-tree-upload" {:error e})
      (errors/handle-analysis-error! db id e))))

(defmethod handler
  ;; "This handler will parse the continuous MCC tree and create HPD interval contours around it if a corresponding trees sample is provided"
  :parse-continuous-tree
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config maps-boxes]}]
  (log/info "handling parse-continuous-tree" args)
  (try
    (let [{:keys [user-id x-coordinate-attribute-name y-coordinate-attribute-name
                  timescale-multiplier most-recent-sampling-date]
           :as continuous-tree}
          (continuous-tree-model/get-tree db {:id id})

          _ (log/info "ContinuousTree retrieved by id" continuous-tree)

          {time-slicer-id                          :id
           hpd-level                               :hpd-level
           burn-in                                 :burn-in
           number-of-intervals                     :number-of-intervals
           contouring-grid-size                    :contouring-grid-size
           relaxed-random-walk-rate-attribute-name :relaxed-random-walk-rate-attribute-name
           :as                                     time-slicer}
          (time-slicer-model/get-time-slicer-by-continuous-tree-id db {:continuous-tree-id id})

          _ (log/info "TimeSlicer retrieved by continuous tree id" time-slicer)

          ;; TODO: parse extension
          tree-object-key  (str user-id "/" id tree-object-extension)
          tree-file-path   (str tmp-dir "/" tree-object-key)
          ;; is it cached on disk?
          _                (when-not (file-exists? tree-file-path)
                             (aws-s3/download-file s3 {:bucket    bucket-name
                                                       :key       tree-object-key
                                                       :dest-path tree-file-path}))
          progress-handler (new-progress-handler (fn [progress]
                                                   (let [progress (round 2 progress)]
                                                     (when (= (mod progress 0.1) 0.0)
                                                       #_(log/debug "continuous tree progress" {:id       id
                                                                                                :progress progress})
                                                       (analysis-model/upsert! db {:id  id
                                                                                   :status   :RUNNING
                                                                                   :progress progress})))))

          ;; call all setters
          continuous-tree-parser (doto (new ContinuousTreeParser)
                                   (.setTreeFilePath tree-file-path)
                                   (.setXCoordinateAttributeName x-coordinate-attribute-name)
                                   (.setYCoordinateAttributeName y-coordinate-attribute-name)
                                   (.setTimescaleMultiplier timescale-multiplier)
                                   (.setMostRecentSamplingDate most-recent-sampling-date))

          ;; NOTE: if we are not using time slicer let this parser report progress
          continuous-tree-parser (if-not time-slicer
                                   (doto continuous-tree-parser
                                     (.registerProgressObserver progress-handler))
                                   continuous-tree-parser)

          continuous-tree-parser-output
          (json/read-str (.parse continuous-tree-parser) :key-fn keyword)

          {:keys [areaAttributes areas]}
          (when time-slicer
            (parse-time-slicer {:id               time-slicer-id
                                :user-id          user-id
                                :db               db
                                :s3               s3
                                :bucket-name      bucket-name
                                :progress-handler progress-handler
                                :parser-settings  {:trait-attribute-name      (subs x-coordinate-attribute-name
                                                                                    0
                                                                                    (longest-common-substring x-coordinate-attribute-name
                                                                                                              y-coordinate-attribute-name))
                                                   :burn-in                   (or burn-in 0.1)
                                                   :number-of-intervals       (or number-of-intervals 10)
                                                   :contouring-grid-size      (or contouring-grid-size 100)
                                                   :hpd-level                 (or hpd-level 0.8)
                                                   :timescale-multiplier      (or timescale-multiplier 1)
                                                   :most-recent-sampling-date most-recent-sampling-date
                                                   :relaxed-random-walk-rate-attribute-name
                                                   relaxed-random-walk-rate-attribute-name}}))

          output-object-key  (str user-id "/" id output-object-extension)
          output-object-path (str tmp-dir "/" output-object-key)
          data               (merge continuous-tree-parser-output
                                    (when time-slicer
                                      {:areaAttributes areaAttributes
                                       :areas          areas}))
          _                  (spit output-object-path (json/write-str data) :append false)
          _                  (aws-s3/upload-file s3 {:bucket    bucket-name
                                                     :key       output-object-key
                                                     :file-path output-object-path})
          url                (aws-s3/build-url aws-config bucket-name output-object-key)
          _                  (log/info "Continuous tree output URL" {:url url})]
      ;; TODO : in a transaction
      (continuous-tree-model/upsert! db {:id              id
                                         :output-file-url url})
      (analysis-model/upsert! db {:id     id
                                  :status :SUCCEEDED
                                  :viewer-url-params (maps/build-viewer-url-params user-id id maps-boxes data)}))
    (catch Exception e
      (log/error "Exception when handling parse-continuous-tree" {:error e})
      (errors/handle-analysis-error! db id e))))

(defn- parse-time-slicer [{:keys [id user-id db s3 bucket-name progress-handler parser-settings]}]
  (let [trees-object-key (str user-id "/" id trees-object-extension)
        trees-file-path  (str tmp-dir "/" trees-object-key)
        ;; is it cached on disk?
        _                (when-not (file-exists? trees-file-path)
                           (aws-s3/download-file s3 {:bucket    bucket-name
                                                     :key       trees-object-key
                                                     :dest-path trees-file-path}))

        {:keys [trait-attribute-name burn-in
                relaxed-random-walk-rate-attribute-name
                number-of-intervals hpd-level contouring-grid-size
                timescale-multiplier
                most-recent-sampling-date]}
        parser-settings

        _ (log/info "time-slicer parser settings" parser-settings)

        parser (doto (new TimeSlicerParser)
                 (.setTreesFilePath trees-file-path)
                 ;; NOTE: this should always hold unless someone switches the usual lat/long convention
                 (.setTraitName trait-attribute-name)
                 (.setBurnIn burn-in)
                 (.setRrwRateName relaxed-random-walk-rate-attribute-name)
                 (.setNumberOfIntervals number-of-intervals)
                 (.setHpdLevel hpd-level)
                 (.setGridSize contouring-grid-size)
                 (.setTimescaleMultiplier timescale-multiplier)
                 (.setMostRecentSamplingDate most-recent-sampling-date)
                 (.registerProgressObserver progress-handler))
        output (.parse parser)]
    (analysis-model/upsert! db {:id     id
                                :status :SUCCEEDED})
    (json/read-str output :key-fn keyword)))

(defmethod handler :parse-bayes-factors
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config maps-boxes]}]
  (log/info "handling parse-bayes-factors" args)
  (try
    (let [_
          (analysis-model/upsert! db {:id     id
                                      :status :RUNNING})
          {:keys [user-id
                  locations-file-url
                  number-of-locations
                  burn-in]}
          (bayes-factor-model/get-bayes-factor-analysis db {:id id})
          ;; TODO: parse extension
          log-object-key   (str user-id "/" id log-object-extension)
          log-file-path    (str tmp-dir "/" log-object-key)
          ;; is it cached on disk?
          _                (when-not (file-exists? log-file-path)
                             (aws-s3/download-file s3 {:bucket    bucket-name
                                                       :key       log-object-key
                                                       :dest-path log-file-path}))
          progress-handler (new-progress-handler (fn [progress]
                                                   (let [progress (round 2 progress)]
                                                     (when (= (mod progress 0.1) 0.0)
                                                       (log/debug "bayes factor progress" {:id       id
                                                                                           :progress progress})
                                                       (analysis-model/upsert! db {:id       id
                                                                                   :status   :RUNNING
                                                                                   :progress progress})))))
          parser
          (match [(nil? locations-file-url) (nil? number-of-locations)]

                 [true false]
                 (doto (new BayesFactorParser)
                   (.setLogFilePath log-file-path)
                   (.setNumberOfGeneratedLocations number-of-locations)
                   (.setBurnIn (double burn-in))
                   (.registerProgressObserver progress-handler))

                 [false true]
                 (let [locations-file-id    (s3-url->id locations-file-url user-id)
                       ;; TODO: parse extension
                       locations-object-key (str user-id "/" locations-file-id locations-object-extension)
                       locations-file-path  (str tmp-dir "/" locations-object-key)
                       ;; is it cached on disk?
                       _                    (when-not (file-exists? locations-file-path)
                                              (aws-s3/download-file s3 {:bucket    bucket-name
                                                                        :key       locations-object-key
                                                                        :dest-path locations-file-path}))]
                   (doto (new BayesFactorParser)
                     (.setLogFilePath log-file-path)
                     (.setLocationsFilePath locations-file-path)
                     (.setBurnIn (double burn-in))
                     (.registerProgressObserver progress-handler)))

                 [false false]
                 (throw (ex-info "Bad input settings"
                                 {:why?   "Can't specify both `locations-file-path` and `number-of-locations`"
                                  :where? ::parse-bayes-factors}))
                 [true true]
                 (throw (ex-info "Bad input settings"
                                 {:why?   "You need to specify one of `log-file-path` and `number-of-locations`"
                                  :where? ::parse-bayes-factors}))
                 :else (throw (Exception. "Unexpected error")))
          {:keys [bayesFactors spreadData]} (-> (.parse parser) (json/read-str :key-fn keyword))
          output-object-key                 (str user-id "/" id output-object-extension)
          output-object-path                (str tmp-dir "/" output-object-key)
          _                                 (spit output-object-path (json/write-str spreadData) :append false)
          _                                 (aws-s3/upload-file s3 {:bucket    bucket-name
                                                                    :key       output-object-key
                                                                    :file-path output-object-path})
          url                               (aws-s3/build-url aws-config bucket-name output-object-key)]
      ;; TODO : in a transaction
      (bayes-factor-model/insert-bayes-factors db {:id            id
                                                   :bayes-factors (json/write-str bayesFactors)})
      (bayes-factor-model/upsert! db {:id              id
                                      :output-file-url url})
      (analysis-model/upsert! db {:id     id
                                  :status :SUCCEEDED
                                  :viewer-url-params (maps/build-viewer-url-params user-id id maps-boxes spreadData)}))
    (catch Exception e
      (log/error "Exception when handling parse-bayes-factors" {:error e})
      (errors/handle-analysis-error! db id e))))

(defn start [{:keys [aws db] :as config}]
  (let [{:keys [workers-queue-url bucket-name]} aws
        sqs                                     (aws-sqs/create-client aws)
        s3                                      (aws-s3/create-client aws)
        db                                      (db/init db)
        maps-boxes                              (maps/load-map-boxes)
        _                                       (log/info (format "Loaded %d maps boxes" (count maps-boxes)))
        context                                 {:s3          s3
                                                 :db          db
                                                 :bucket-name bucket-name
                                                 :aws-config  aws
                                                 :maps-boxes  maps-boxes}]

    (log/info "Starting worker listener" config)
    (loop []
      (try
        ;; If the queue is empty, wait for 2 seconds and poll again
        ;; (log/debug "Polling...")
        (when-let [{:keys [body receipt-handle]} (aws-sqs/get-next-message sqs workers-queue-url)]
          (handler body context)
          (aws-sqs/ack-message sqs workers-queue-url receipt-handle))
        (Thread/sleep 2000)
        (catch Exception e
          (log/error "Error processing a message" {:error e})))
      (recur))))

(defstate listener
  :start (start (mount/args)))
