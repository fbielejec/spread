(ns api.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [shared.utils :refer [get-env-variable]]))

(defn try-secrets [path]
  (try
    (-> (io/resource path)
        slurp
        edn/read-string)
    (catch Exception _
      ;; ignore any errors, like missing file
      )))

(defn load! []
  (let [environment (or (get-env-variable "SPREAD_ENV") "dev")
        dev-env?    (= "dev" environment)
        {{:keys [client-secret]} :google
         :keys                   [private-key]}
        (when dev-env?
          (try-secrets "secrets.edn"))]

    {:env     environment
     :version "1.0.3"
     :logging {:level (or (keyword (get-env-variable "LOGGING_LEVEL")) :debug)}
     :api     {:port            (Integer/parseInt (or (get-env-variable "API_PORT") "3001"))
               :host (or (get-env-variable "API_HOST") "0.0.0.0")
               :allowed-origins #{"http://localhost:8020"
                                  "http://127.0.0.1:3001"
                                  "https://studio.apollographql.com"
                                  "https://spreadviz.org"}}
     :aws     (cond-> {:region (when-not dev-env?
                                 (get-env-variable "API_AWS_REGION"))
                       :access-key-id  (or (get-env-variable "API_AWS_ACCESS_KEY_ID") "AKIAIOSFODNN7EXAMPLE")
                       :secret-access-key (or (get-env-variable "API_AWS_SECRET_ACCESS_KEY") "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                       :bucket-name (or (get-env-variable "BUCKET_NAME") "spread-dev-uploads")
                       :workers-queue-url (or (get-env-variable "WORKERS_QUEUE_URL") "http://localhost:9324/queue/workers")}
                dev-env? (assoc :sqs-host "localhost"
                                :sqs-port 9324
                                :s3-host "127.0.0.1"
                                :s3-port 9000))
     :db
     {:dbname   (or (get-env-variable "DB_DATABASE") "spread")
      :port     (or (get-env-variable "DB_PORT") 3306)
      :user     (or (get-env-variable "DB_USER") "root")
      :password (or (get-env-variable "DB_PASSWORD") "Pa55w0rd")
      :host     (or (get-env-variable "DB_HOST") "127.0.0.1")}

     :google
     {:client-id     (or (get-env-variable "GOOGLE_CLIENT_ID") "806052757605-5sbubbk9ubj0tq95dp7b58v36tscqv1r.apps.googleusercontent.com")
      :client-secret (or (get-env-variable "GOOGLE_CLIENT_SECRET") client-secret)}

     :public-key
     (or (get-env-variable "PUBLIC_KEY")
         "-----BEGIN PUBLIC KEY-----\nMFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJliLjOIAqGbnjGBM1RJml/l0MHayaRH\ncgEg00O9wBYvoNXrstFSzKTCKtG5MayUKgdG7C/98nu/TEzhvRFjINcCAwEAAQ==\n-----END PUBLIC KEY-----\n")
     :private-key (or (get-env-variable "PRIVATE_KEY") private-key)}))
