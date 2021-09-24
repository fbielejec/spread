(ns ui.main
  (:require [day8.re-frame.forward-events-fx]
            [mount.core :as mount]
            [re-frame.core :as re-frame]
            [reagent-material-ui.core.css-baseline :refer [css-baseline]]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [shared.events]
            [shared.subs]
            [taoensso.timbre :as log]
            [ui.analysis-results.page]
            [ui.config :as config]
            [ui.documentation.page]
            [ui.events]                        
            [ui.home.page]
            [ui.logging :as logging]
            [ui.new-analysis.page]
            [ui.router.component :refer [router]]
            [ui.router.core :as router]
            [ui.splash.page]
            [ui.storage]
            [ui.utils]
            [ui.websocket-fx :as websocket]
            ))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn ^:dev/before-load stop []
  (log/debug "Stopping...")
  (mount/stop))

(defn ^:dev/after-load start []
  (let [{:keys [version] :as config} (config/load)]
    (js/console.log (str "Starting v" version " ..."))
    (js/console.log (str "Config " config))
    (-> (mount/only #{#'logging/logging
                      #'router/router})
        (mount/with-args config)
        (mount/start))
    (re-frame/dispatch-sync [:general/initialize config])
    (rdom/render [:<>
                  [css-baseline]
                  [router]]
                 (.getElementById js/document "app")
                 functional-compiler)))

(defn ^:export init []
  (start))

(comment
  (re-frame/dispatch [:utils/app-db])
  @(re-frame/subscribe [::websocket/status :default])
  (re-frame/dispatch [:graphql/ws-authorize])
  @(re-frame/subscribe [::websocket/open-subscriptions :default])
  (re-frame/dispatch [:router/navigate :route/home]))
