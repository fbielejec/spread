(ns tests.integration.bayes-factor-test
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [use-fixtures deftest is]]
            [taoensso.timbre :as log]
            [tests.integration.utils :refer [run-query db-fixture]]))

(use-fixtures :once db-fixture)

(deftest continuous-tree-test
  (let [

        [log-url locations-url] (get-in (run-query {:query
                                                    "mutation GetUploadUrls($files: [File]) {
                                                        getUploadUrls(files: $files)
                                                      }"
                                                    :variables {:files [{:name      "H5N1_HA_discrete_rateMatrix"
                                                                         :extension "log"}
                                                                        {:name      "locationCoordinates_H5N1"
                                                                         :extension "txt"}]}})
                                        [:data :getUploadUrls])
        _                       (http/put log-url {:body (io/file "src/test/resources/bayesFactor/H5N1_HA_discrete_rateMatrix.log")})
        _                       (http/put locations-url {:body (io/file "src/test/resources/bayesFactor/locationCoordinates_H5N1")})

        {:keys [id status]} (get-in (run-query {:query
                                                "mutation UploadBayesFactor($logUrl: String!,
                                                                            $locationsUrl: String!) {
                                                   uploadBayesFactorAnalysis(logFileUrl: $logUrl,
                                                                             locationsFileUrl: $locationsUrl) {
                                                     id
                                                     status
                                                  }
                                                }"
                                                :variables {:logUrl       (-> log-url
                                                                              (string/split  #"\?")
                                                                              first)
                                                            :locationsUrl (-> locations-url
                                                                              (string/split  #"\?")
                                                                              first)}})
                                    [:data :uploadBayesFactorAnalysis])

        _ (is :DATA_UPLOADED (keyword status))

        {:keys [status]} (get-in (run-query {:query
                                             "mutation UpdateBayesFactor($id: ID!,
                                                                  $burnIn: Float!) {
                                                updateBayesFactorAnalysis(id: $id,
                                                                          burnIn: $burnIn) {
                                                  status
                                                }
                                              }"
                                             :variables {:id     id
                                                         :burnIn 0.1}})
                                 [:data :updateBayesFactorAnalysis])

        ]

    (log/debug "response" {:id id :status status})



    (is false)

    ))
