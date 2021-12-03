(ns ui.events
  (:require
   [day8.re-frame.http-fx]
   [re-frame.core :as re-frame :refer [reg-event-fx]]
   [ui.events.analysis-results :as events.analysis-results]
   [ui.events.bayes-factor :as events.bayes-factor]
   [ui.events.continuous-mcc-tree :as events.continuous-mcc-tree]
   [ui.events.discrete-mcc-tree :as events.discrete-mcc-tree]
   [ui.events.general :as events.general]
   [ui.events.graphql :as events.graphql]
   [ui.events.home :as events.home]
   [ui.events.new-analysis :as events.new-analysis]
   [ui.events.router :as events.router]
   [ui.events.splash :as events.splash]
   [ui.events.utils :as events.utils]
   [ui.events.websocket :as events.websocket]
   ))

;;;;;;;;;;;;;;;;;;
;; Utils events ;;
;;;;;;;;;;;;;;;;;;

(reg-event-fx :utils/app-db events.utils/app-db)

;;;;;;;;;;;;;;;;;;;;
;; General events ;;
;;;;;;;;;;;;;;;;;;;;

(reg-event-fx :do-nothing (constantly nil))
(reg-event-fx :user-analysis-loaded (constantly nil))
(reg-event-fx :log-error (fn [_ ev]
                           (js/console.error ev)))

;;;;;;;;;;;;;;;;;;;;
;; Graphql events ;;
;;;;;;;;;;;;;;;;;;;;

(reg-event-fx :graphql/response events.graphql/response)
(reg-event-fx :graphql/query [(re-frame/inject-cofx :localstorage)] events.graphql/query)
(reg-event-fx :graphql/ws-authorized (constantly nil))
(reg-event-fx :graphql/ws-authorize [(re-frame/inject-cofx :localstorage)] events.graphql/ws-authorize)
(reg-event-fx :graphql/ws-authorize-failed events.graphql/ws-authorize-failed)
(reg-event-fx :graphql/subscription-response events.graphql/subscription-response)
(reg-event-fx :graphql/subscription events.graphql/subscription)
(reg-event-fx :graphql/unsubscribe events.graphql/unsubscribe)

;;;;;;;;;;;;;;;;;;;;;;;
;; General UI events ;;
;;;;;;;;;;;;;;;;;;;;;;;

(reg-event-fx :general/active-page-changed events.general/active-page-changed)
(reg-event-fx :general/initialize [(re-frame/inject-cofx :localstorage)] events.general/initialize)
(reg-event-fx :general/logout [(re-frame/inject-cofx :localstorage)] events.general/logout)
(reg-event-fx :general/set-search events.general/set-search)
(reg-event-fx :general/query-analysis events.general/query-analysis)
(reg-event-fx :general/copy-analysis-settings events.general/copy-settings)
(reg-event-fx :general/paste-analysis-settings events.general/paste-settings)

;;;;;;;;;;;;;;;;;
;; Home events ;;
;;;;;;;;;;;;;;;;;

(reg-event-fx :home/initialize-page events.home/initialize-page)
(reg-event-fx :home/initial-query events.home/initial-query)

;;;;;;;;;;;;;;;;;;;
;; Splash events ;;
;;;;;;;;;;;;;;;;;;;

(reg-event-fx :splash/initialize-page [(re-frame/inject-cofx :localstorage)] events.splash/initialize-page)
(reg-event-fx :splash/google-login events.splash/google-login)
(reg-event-fx :splash/send-login-email events.splash/send-login-email)
(reg-event-fx :splash/email-login events.splash/email-login)
(reg-event-fx :splash/login-success [(re-frame/inject-cofx :localstorage)] events.splash/login-success)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Analysis results events ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx :analysis-results/initialize-page events.analysis-results/initialize-page)
(re-frame/reg-event-fx :analysis-results/initial-query events.analysis-results/initial-query)
(re-frame/reg-event-fx :analysis-results/export-bayes-table-to-csv events.analysis-results/export-bayes-table-to-csv)

;;;;;;;;;;;;;;;;;;;;;;;;;
;; New analysis events ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx :new-analysis/initialize-page events.new-analysis/initialize-page)
(re-frame/reg-event-fx :new-analysis/initial-query events.new-analysis/initial-query)

(re-frame/reg-event-fx :continuous-mcc-tree/on-tree-file-selected events.continuous-mcc-tree/on-tree-file-selected)
(re-frame/reg-event-fx :continuous-mcc-tree/upload-tree-file events.continuous-mcc-tree/upload-tree-file)
(re-frame/reg-event-fx :continuous-mcc-tree/tree-file-upload-progress events.continuous-mcc-tree/tree-file-upload-progress)
(re-frame/reg-event-fx :continuous-mcc-tree/tree-file-upload-success events.continuous-mcc-tree/tree-file-upload-success)
(re-frame/reg-event-fx :continuous-mcc-tree/delete-tree-file events.continuous-mcc-tree/delete-tree-file)

(re-frame/reg-event-fx :continuous-mcc-tree/on-trees-file-selected events.continuous-mcc-tree/on-trees-file-selected)
(re-frame/reg-event-fx :continuous-mcc-tree/upload-trees-file events.continuous-mcc-tree/upload-trees-file)
(re-frame/reg-event-fx :continuous-mcc-tree/trees-file-upload-progress events.continuous-mcc-tree/trees-file-upload-progress)
(re-frame/reg-event-fx :continuous-mcc-tree/trees-file-upload-success events.continuous-mcc-tree/trees-file-upload-success)
(re-frame/reg-event-fx :continuous-mcc-tree/delete-trees-file events.continuous-mcc-tree/delete-trees-file)

(re-frame/reg-event-fx :continuous-mcc-tree/start-analysis events.continuous-mcc-tree/start-analysis)
(re-frame/reg-event-fx :continuous-mcc-tree/set-readable-name events.continuous-mcc-tree/set-readable-name)
(re-frame/reg-event-fx :continuous-mcc-tree/set-y-coordinate events.continuous-mcc-tree/set-y-coordinate)
(re-frame/reg-event-fx :continuous-mcc-tree/set-x-coordinate events.continuous-mcc-tree/set-x-coordinate)
(re-frame/reg-event-fx :continuous-mcc-tree/set-most-recent-sampling-date events.continuous-mcc-tree/set-most-recent-sampling-date)
(re-frame/reg-event-fx :continuous-mcc-tree/set-time-scale-multiplier events.continuous-mcc-tree/set-time-scale-multiplier)
(re-frame/reg-event-fx :continuous-mcc-tree/reset events.continuous-mcc-tree/reset)

(re-frame/reg-event-fx :discrete-mcc-tree/on-tree-file-selected events.discrete-mcc-tree/on-tree-file-selected)
(re-frame/reg-event-fx :discrete-mcc-tree/s3-tree-file-upload events.discrete-mcc-tree/s3-tree-file-upload)
(re-frame/reg-event-fx :discrete-mcc-tree/tree-file-upload-progress events.discrete-mcc-tree/tree-file-upload-progress)
(re-frame/reg-event-fx :discrete-mcc-tree/tree-file-upload-success events.discrete-mcc-tree/tree-file-upload-success)
(re-frame/reg-event-fx :discrete-mcc-tree/delete-tree-file events.discrete-mcc-tree/delete-tree-file)
(re-frame/reg-event-fx :discrete-mcc-tree/on-locations-file-selected events.discrete-mcc-tree/on-locations-file-selected)
(re-frame/reg-event-fx :discrete-mcc-tree/s3-locations-file-upload events.discrete-mcc-tree/s3-locations-file-upload)
(re-frame/reg-event-fx :discrete-mcc-tree/locations-file-upload-progress events.discrete-mcc-tree/locations-file-upload-progress)
(re-frame/reg-event-fx :discrete-mcc-tree/locations-file-upload-success events.discrete-mcc-tree/locations-file-upload-success)
(re-frame/reg-event-fx :discrete-mcc-tree/set-readable-name events.discrete-mcc-tree/set-readable-name)
(re-frame/reg-event-fx :discrete-mcc-tree/set-locations-attribute events.discrete-mcc-tree/set-locations-attribute)
(re-frame/reg-event-fx :discrete-mcc-tree/set-most-recent-sampling-date events.discrete-mcc-tree/set-most-recent-sampling-date)
(re-frame/reg-event-fx :discrete-mcc-tree/set-time-scale-multiplier events.discrete-mcc-tree/set-time-scale-multiplier)
(re-frame/reg-event-fx :discrete-mcc-tree/start-analysis events.discrete-mcc-tree/start-analysis)
(re-frame/reg-event-fx :discrete-mcc-tree/delete-locations-file events.discrete-mcc-tree/delete-locations-file)
(re-frame/reg-event-fx :discrete-mcc-tree/reset events.discrete-mcc-tree/reset)

(re-frame/reg-event-fx :bayes-factor/on-log-file-selected events.bayes-factor/on-log-file-selected)
(re-frame/reg-event-fx :bayes-factor/s3-log-file-upload events.bayes-factor/s3-log-file-upload)
(re-frame/reg-event-fx :bayes-factor/log-file-upload-progress events.bayes-factor/log-file-upload-progress)
(re-frame/reg-event-fx :bayes-factor/log-file-upload-success events.bayes-factor/log-file-upload-success)
(re-frame/reg-event-fx :bayes-factor/delete-log-file events.bayes-factor/delete-log-file)
(re-frame/reg-event-fx :bayes-factor/on-locations-file-selected events.bayes-factor/on-locations-file-selected)
(re-frame/reg-event-fx :bayes-factor/s3-locations-file-upload events.bayes-factor/s3-locations-file-upload)
(re-frame/reg-event-fx :bayes-factor/locations-file-upload-progress events.bayes-factor/locations-file-upload-progress)
(re-frame/reg-event-fx :bayes-factor/locations-file-upload-success events.bayes-factor/locations-file-upload-success)
(re-frame/reg-event-fx :bayes-factor/delete-locations-file events.bayes-factor/delete-locations-file)
(re-frame/reg-event-fx :bayes-factor/set-burn-in events.bayes-factor/set-burn-in)
(re-frame/reg-event-fx :bayes-factor/set-readable-name events.bayes-factor/set-readable-name)
(re-frame/reg-event-fx :bayes-factor/start-analysis events.bayes-factor/start-analysis)
(re-frame/reg-event-fx :bayes-factor/reset events.bayes-factor/reset)

;;;;;;;;;;;;;;;;;;;;;;;
;; Websockets events ;;
;;;;;;;;;;;;;;;;;;;;;;;

(reg-event-fx :websocket/connect events.websocket/connect)
(reg-event-fx :websocket/disconnect events.websocket/disconnect)
(reg-event-fx :websocket/connected events.websocket/connected)
(reg-event-fx :websocket/disconnected events.websocket/disconnected)
(reg-event-fx :websocket/request events.websocket/request)
(reg-event-fx :websocket/request-response events.websocket/request-response)
(reg-event-fx :websocket/request-timeout events.websocket/request-timeout)
(reg-event-fx :websocket/subscribe events.websocket/subscribe)
(reg-event-fx :websocket/subscription-message events.websocket/subscription-message)
(reg-event-fx :websocket/unsubscribe events.websocket/unsubscribe)
(reg-event-fx :websocket/subscription-closed events.websocket/subscription-closed)
(reg-event-fx :websocket/push events.websocket/push)

;;;;;;;;;;;;;;;;;;;
;; Router events ;;
;;;;;;;;;;;;;;;;;;;

(reg-event-fx :router/start events.router/interceptors events.router/start)
(reg-event-fx :router/active-page-change events.router/interceptors events.router/active-page-change)
(reg-event-fx :router/active-page-changed events.router/interceptors events.router/active-page-changed)
(reg-event-fx :router/watch-active-page events.router/interceptors events.router/watch-active-page)
(reg-event-fx :router/unwatch-active-page events.router/interceptors events.router/unwatch-active-page)
(reg-event-fx :router/navigate events.router/interceptors events.router/navigate)
(reg-event-fx :router/stop events.router/interceptors events.router/stop)

(comment
  (re-frame/dispatch [:utils/app-db]))
