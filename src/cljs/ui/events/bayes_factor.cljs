(ns ui.events.bayes-factor
  (:require [clojure.string :as string]
            [ui.s3 :as s3]
            [ui.utils :as ui-utils :refer [>evt dissoc-in]]))

(defn on-log-file-selected [_ [_ file-with-meta]]
  (let [{:keys [filename]} file-with-meta
        splitted           (string/split filename ".")
        fname              (first splitted)]
    {:dispatch [:graphql/query {:query
                                "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                :variables  {:filename fname :extension "log"}
                                :on-success [:bayes-factor/s3-log-file-upload file-with-meta]}]}))

(defn s3-log-file-upload [{:keys [db]} [_ {:keys [data filename]} response]]
  (let [url (-> response :data :getUploadUrls first)]
    {:db         (assoc-in db [:new-analysis :bayes-factor :upload-status] "UPLOADING")
     ::s3/upload {:url             url
                  :data            data
                  :on-success      #(>evt [:bayes-factor/log-file-upload-success {:url      url
                                                                                  :filename filename}])
                  ;; TODO : handle error
                  :on-error        #(prn "ERROR" %)
                  :handle-progress (fn [sent total]
                                     (>evt [:bayes-factor/log-file-upload-progress (/ sent total)]))}}))

(defn log-file-upload-progress [{:keys [db]} [_ progress]]
  {:db (assoc-in db [:new-analysis :bayes-factor :log-file-upload-progress] progress)})

(defn delete-log-file [{:keys [db]}]
  (let [id (get-in db [:new-analysis :bayes-factor :id])]
    {:dispatch [:graphql/query {:query
                                "mutation DeleteAnalysisMutation($analysisId: ID!) {
                                   deleteAnalysis(id: $analysisId) {
                                          id
                                        }
                                     }"
                                :variables {:analysisId id}}]
     :db       (dissoc-in db [:new-analysis :bayes-factor])}))

(defn delete-locations-file [{:keys [db]}]
  ;; NOTE : we just delete the object from S3 and dissoc the app-db values
  ;; there is no need to change the analysis settings in the DB as they are not set yet
  ;; this happens only when the analysis is started
  (let [{:keys [locations-file-url]} (get-in db [:new-analysis :bayes-factor])]
    {:dispatch [:graphql/query {:query
                                "mutation DeleteFile($url: String!) {
                                     deleteFile(url: $url) {
                                       key
                                     }
                                   }"
                                :variables {:url locations-file-url}}]
     :db (-> db
             (dissoc-in [:new-analysis :bayes-factor :locations-file])
             (dissoc-in [:new-analysis :bayes-factor :locations-file-url])
             (dissoc-in [:new-analysis :bayes-factor :locations-file-upload-progress]))}))

(defn log-file-upload-success [{:keys [db]} [_ {:keys [url filename]}]]
  (let [[url _]       (string/split url "?")
        readable-name (first (string/split filename "."))]
    {:dispatch [:graphql/query {:query     "mutation UploadBayesFactor($logUrl: String!) {
                                                   uploadBayesFactorAnalysis(logFileUrl: $logUrl) {
                                                     id
                                                     status
                                                  }
                                                }"
                                :variables {:logUrl url}}]
     :db       (-> db
                   (assoc-in [:new-analysis :bayes-factor :upload-status] "UPLOADED")
                   (assoc-in [:new-analysis :bayes-factor :log-file] filename)
                   ;; default name: file name root
                   (assoc-in [:new-analysis :bayes-factor :readable-name] readable-name))}))

(defn on-locations-file-selected [_ [_ file-with-meta]]
  (let [{:keys [filename]} file-with-meta
        splitted           (string/split filename ".")
        fname              (first splitted)]
    {:dispatch [:graphql/query {:query
                                "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                :variables  {:filename fname :extension "txt"}
                                :on-success [:bayes-factor/s3-locations-file-upload file-with-meta]}]}))

(defn s3-locations-file-upload [{:keys [db]} [_ {:keys [data filename]} response]]
  (let [url (-> response :data :getUploadUrls first)]
    {:db         (assoc-in db [:new-analysis :bayes-factor :status] "UPLOADING")
     ::s3/upload {:url             url
                  :data            data
                  :on-success      #(>evt [:bayes-factor/locations-file-upload-success {:url      url
                                                                                        :filename filename}])
                  ;; TODO : handle error
                  :on-error        #(prn "ERROR" %)
                  :handle-progress (fn [sent total]
                                     (>evt [:bayes-factor/locations-file-upload-progress (/ sent total)]))}}))

(defn locations-file-upload-progress [{:keys [db]} [_ progress]]
  {:db (assoc-in db [:new-analysis :bayes-factor :locations-file-upload-progress] progress)})

(defn locations-file-upload-success [{:keys [db]} [_ {:keys [url filename]}]]
  (let [[url _] (string/split url "?")]
    {:db (-> db
             (assoc-in [:new-analysis :bayes-factor :status] "UPLOADED")
             (assoc-in [:new-analysis :bayes-factor :locations-file-url] url)
             (assoc-in [:new-analysis :bayes-factor :locations-file] filename))}))

(defn set-readable-name [{:keys [db]} [_ readable-name]]
  {:db (assoc-in db [:new-analysis :bayes-factor :readable-name] readable-name)})

(defn set-burn-in [{:keys [db]} [_ burn-in]]
  {:db (assoc-in db [:new-analysis :bayes-factor :burn-in] burn-in)})

(defn start-analysis [{:keys [db]} [_ {:keys [readable-name locations-file-url burn-in]}]]
  (let [id (get-in db [:new-analysis :bayes-factor :id])]
    {:db       (assoc-in db [:analysis id :readable-name] readable-name)
     :dispatch [:graphql/query {:query
                                "mutation UpdateBayesFactor($id: ID!,
                                                            $name: String!,
                                                            $locationsFileUrl: String!,
                                                            $burnIn: Float!) {
                                            updateBayesFactorAnalysis(id: $id,
                                                                      readableName: $name,
                                                                      locationsFileUrl: $locationsFileUrl,
                                                                      burnIn: $burnIn) {
                                                id
                                                status
                                              }
                                            }"
                                :variables {:id               id
                                            :name             readable-name
                                            :locationsFileUrl locations-file-url
                                            :burnIn           burn-in}}]}))
