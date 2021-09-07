(ns ui.config
  (:require [shared.macros :refer [get-env-variable]]))

(def environment (get-env-variable "SPREAD_ENV"))
(def google-client-id (get-env-variable "GOOGLE_CLIENT_ID" :required))
(def public-key (get-env-variable "PUBLIC_KEY" :required))
(def version "0.1.0")

(def default-config
  {:version "1.0.1"
   :logging
   {:level    :info
    :console? true
    :sentry   false}

   :router
   {:routes        [["/" :route/splash]
                    ["/home" :route/home]
                    ["/new-analysis" :route/new-analysis]
                    ["/analysis-results" :route/analysis-results]
                    ["/documentation" :route/documentation]
                    ["/map" :route/map]]
    :default-route :route/splash
    :scroll-top?   true
    :html5?        true}

   :graphql
   {:ws-url "ws://127.0.0.1:3001/ws"
    :url    "http://127.0.0.1:3001/api"}

   :root-url "http://localhost:8020"
   :analysis-viewer-url "http://localhost:8021"
   :google
   {:client-id    google-client-id
    :redirect-uri "http://localhost:8020/?auth=google"}

   :public-key public-key})

(def dev-config
  (-> default-config
      (assoc-in [:logging :level] :debug)))

(def prod-config
  (-> default-config
      (assoc-in [:logging :level] :info)
      (assoc-in [:graphql :ws-url] "wss://api.spreadviz.org/ws")
      (assoc-in [:graphql :url] "https://api.spreadviz.org/api")
      (assoc :root-url "https://spreadviz.org")
      (assoc-in [:google :redirect-url] "https://spreadviz.org/?auth=google")))

(defn load []
  (case environment
    "dev" dev-config
    "prod"  prod-config
    dev-config))
