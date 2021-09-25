(ns aws.s3
  (:require [aws.utils :refer [throw-on-error]]
            [clojure.java.io :as io]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [taoensso.timbre :as log])
  (:import (software.amazon.awssdk.services.s3.model PutObjectRequest)
           (software.amazon.awssdk.services.s3.presigner.model PutObjectPresignRequest)
           (software.amazon.awssdk.services.s3.presigner S3Presigner)
           (software.amazon.awssdk.auth.credentials AwsBasicCredentials)
           (software.amazon.awssdk.auth.credentials StaticCredentialsProvider)
           (software.amazon.awssdk.regions Region)
           (java.time Duration)
           (java.net URI)))

(defn create-client

  "Creates a aws S3 client. Needed for calling all s3 functions."

  [{:keys [access-key-id secret-access-key region s3-host s3-port]}]
  (aws/client (cond-> {:api :s3
                       :credentials-provider (credentials/basic-credentials-provider
                                              {:access-key-id access-key-id
                                               :secret-access-key secret-access-key})}
                region (assoc :region region)
                ;; only for dev
                s3-host (assoc :endpoint-override {:protocol :http
                                                   :hostname s3-host
                                                   :port s3-port})

                ;; if we are in testing lets smash a real region
                ;; the aws/invoke fails with "No region found by any region provider."
                ;; if we don't provide a real one. Providing "spread-test-1" doesn't work
                s3-host (assoc :region "us-east-1"))))

(defn create-presigner

  "Creates a aws S3 url presigner. Needed for generating presigned URLs."

  [{:keys [access-key-id secret-access-key region s3-host s3-port]}]
  (let [credentials (AwsBasicCredentials/create access-key-id secret-access-key)
        credentialsProvider (StaticCredentialsProvider/create credentials)]
    (if region
      (-> (S3Presigner/builder)
          (.region (Region/of region))
          (.credentialsProvider credentialsProvider)
          (.build))
      ;; only for dev
      (-> (S3Presigner/builder)
          ;; hack, else it throws
          (.region (Region/of "us-east-2"))
          (.endpointOverride (URI/create (str "http://" s3-host ":" s3-port)))
          (.credentialsProvider credentialsProvider)
          (.build)))))

(defn list-buckets [s3]
  (aws/invoke s3 {:op :ListBuckets}))

(defn create-bucket [s3 {:keys [bucket-name]}]
  (aws/invoke s3 {:op :CreateBucket
                  :request {:Bucket bucket-name
                            :ACL "public-read"}}))

(defn get-signed-url [s3-presigner {:keys [bucket-name key]}]
  (let [putObjectRequest (-> (PutObjectRequest/builder)
                             (.bucket bucket-name)
                             (.key key)
                             (.build))
        putObjectPresignRequest (-> (PutObjectPresignRequest/builder)
                                    (.signatureDuration (Duration/ofMinutes 15))
                                    (.putObjectRequest putObjectRequest)
                                    (.build))
        presignedPutObjectRequest (-> s3-presigner
                                      (.presignPutObject putObjectPresignRequest))]
    (-> presignedPutObjectRequest (.url))))

(defn upload-file

  "Uploads the file at `file-path` under a s3 `bucket` under `key`."

  [s3 {:keys [bucket key file-path]}]
  (-> (aws/invoke s3 {:op :PutObject
                      :request {:Bucket bucket
                                :Key key
                                :Body (-> file-path
                                          io/file
                                          io/input-stream)}})
      (throw-on-error {:api :s3 :fn ::upload-file})))

(defn download-file

  "Downloads the file from s3 `bucket` and `key` and
  stores it in a file at `dest-path`."

  [s3 {:keys [bucket key dest-path]}]
  (log/info "Downloading file from s3" {:bucket bucket
                                        :key key
                                        :saving-in dest-path})
  (io/make-parents dest-path)
  (-> (aws/invoke s3 {:op :GetObject
                      :request {:Bucket bucket
                                :Key key}})
      (throw-on-error {:api :s3 :fn ::download-file})
      :Body
      (io/copy (io/file dest-path))))

(defn delete-file

  "Deletes the file from s3 `bucket` and `key` and"

  [s3 {:keys [bucket key]}]
  (log/info "Deleting file from s3" {:bucket bucket
                                     :key    key})
  (-> (aws/invoke s3 {:op      :DeleteObject
                      :request {:Bucket bucket
                                :Key    key}})
      (throw-on-error {:api :s3 :fn ::delete-file})))

(defn build-url [aws-config bucket key]
  (if-let [s3-host (:s3-host aws-config)]
    ;; this is MinIO
    (format "%s:%s/%s/%s"
            s3-host
            (:s3-port aws-config)
            bucket
            key)

    ;; no endpoint means aws S3
    (format "https://%s.s3.%s.amazonaws.com/%s"
            bucket
            (:region aws-config)
            key)))
