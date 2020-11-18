(ns worker.listener
  (:require
   [aws.sqs :as aws-sqs]
   [clojure.core.async :refer [close! thread <!! >!! <! go-loop go] :as async]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]
   ))

;; (defonce control-ch (async/chan))

;; TODO : invoke parser, process message body
(defn handle-message [body]
  (log/info "Handling message" {:msg body} )
  (Thread/sleep (or (:sleep body) 3000)) ;; long process
  )

;; TODO : implement
(defn stop [this]
  (log/debug "@@@ stop" {:t this})
  (close! this))

(defn start [config]
  (let [{:keys [aws]} config
        {:keys [workers-queue-url]} aws
        sqs (aws-sqs/create-client aws)]
    (log/info "Starting worker listener")
    (<!! (go-loop []
           (try
             ;; If the queue is empty, wait for 2 seconds and poll again
             (log/debug "Polling...")
             (if-let [{:keys [body receipt-handle]} (aws-sqs/get-next-message sqs workers-queue-url)]
               (do
                 (handle-message body)
                 (aws-sqs/ack-message sqs workers-queue-url receipt-handle)))
             (<! (async/timeout 2000))
             (catch Exception e
               (log/error "Error processing a message" {:error e})))
           (recur)))))

(defstate listener
  :start (start (mount/args))
  :stop (stop listener))
