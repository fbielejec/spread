(ns api.server
  (:require [api.auth :as auth]
            [api.db :as db]
            [api.mutations :as mutations]
            [api.resolvers :as resolvers]
            [aws.s3 :as aws-s3]
            [aws.sqs :as aws-sqs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [io.pedestal.interceptor :refer [interceptor]]
            [com.walmartlabs.lacinia.pedestal :refer [inject]]
            [com.walmartlabs.lacinia.pedestal2 :as pedestal]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as lacinia-util]
            [io.pedestal.http :as http]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

(defn auth-decorator [resolver-fn]
  (fn [{:keys [headers] :as application-context} args value]
    (if-let [user-id (auth/token->user-id (get headers "Authorization"))]
      (resolver-fn (merge application-context {:authed-user-id user-id}) args value)
      (throw (Exception. "Authorization required")))))

#_(defn context-decorator [resolver-fn context]
  (fn [application-context args value]
    (resolver-fn (merge application-context context) args value)))

#_(defn resolver-map [context]
  {:query/getParserExecution (auth-decorator (context-decorator resolvers/get-parser-execution context))
   :mutation/getUploadUrls (auth-decorator (context-decorator mutations/get-upload-urls context))
   :mutation/uploadContinuousTree (auth-decorator (context-decorator mutations/upload-continuous-tree context))
   :mutation/startParserExecution (auth-decorator (context-decorator mutations/start-parser-execution context))
   })

(defn resolver-map [context]
  {:query/getParserExecution (auth-decorator resolvers/get-parser-execution )
   :mutation/getUploadUrls (auth-decorator mutations/get-upload-urls )
   :mutation/uploadContinuousTree (auth-decorator mutations/upload-continuous-tree )
   :mutation/startParserExecution (auth-decorator mutations/start-parser-execution )
   })

(defn ^:private context-interceptor
  [extra-context]
  (interceptor
   {:name ::extra-context
    :enter (fn [context]

             ;; (log/debug "@@@ context-interceptor" {:ctx extra-context})

             (assoc-in context [:request :lacinia-app-context] extra-context)

             #_(merge lacinia-context extra-context))}))

(defn ^:private interceptors
  [schema extra-context]
  (-> (pedestal/default-interceptors schema nil)
      (inject (context-interceptor extra-context) :after ::pedestal/inject-app-context)))

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string))

(defn stop [this]
  (http/stop this))

(defn start [{:keys [api aws db env] :as config}]
  (let [{:keys [port]} api
        {:keys [workers-queue-url bucket-name]} aws
        schema (load-schema)
        sqs (aws-sqs/create-client aws)
        s3 (aws-s3/create-client aws)
        s3-presigner (aws-s3/create-presigner aws)
        db (db/init db)
        context {:sqs sqs
                 :s3 s3
                 :s3-presigner s3-presigner
                 :db db
                 :workers-queue-url workers-queue-url
                 :bucket-name bucket-name}
        compiled-schema (-> schema
                            (lacinia-util/attach-resolvers (resolver-map context))
                            schema/compile)

        interceptors (interceptors compiled-schema context)
        routes #{["/api" :post interceptors :route-name ::api]}

        ;; service (pedestal2/default-service compiled-schema {:port (Integer/parseInt port)})
        ;; runnable-service (http/create-server service)

        runnable-service
        (-> {:env (keyword env)
             ::http/routes routes
             ::http/port (Integer/parseInt port)
             ::http/type :jetty
             ::http/join? false}
            http/create-server
            http/start)

        ]
    (log/info "Starting server" config)

    (when-not (contains? (set (:Buckets (aws-s3/list-buckets s3))) bucket-name)
      (aws-s3/create-bucket s3 {:bucket-name bucket-name}))

    (http/start runnable-service)
    runnable-service))

#_(defn start [{:keys [api aws db] :as config}]
  (let [{:keys [port]} api
        {:keys [workers-queue-url bucket-name]} aws
        schema (load-schema)
        sqs (aws-sqs/create-client aws)
        s3 (aws-s3/create-client aws)
        s3-presigner (aws-s3/create-presigner aws)
        db (db/init db)
        context {:sqs sqs
                 :s3 s3
                 :s3-presigner s3-presigner
                 :db db
                 :workers-queue-url workers-queue-url
                 :bucket-name bucket-name}
        compiled-schema (-> schema
                            (lacinia-util/attach-resolvers (resolver-map context))
                            schema/compile)
        service (pedestal/default-service compiled-schema {:port (Integer/parseInt port)})
        runnable-service (http/create-server service)]
    (log/info "Starting server" config)

    (when-not (contains? (set (:Buckets (aws-s3/list-buckets s3))) bucket-name)
      (aws-s3/create-bucket s3 {:bucket-name bucket-name}))

    (http/start runnable-service)
    runnable-service))

(defstate server
  :start (start (mount/args))
  :stop (stop server))
