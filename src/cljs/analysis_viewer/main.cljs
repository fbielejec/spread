(ns analysis-viewer.main
  (:require [analysis-viewer.events]
            [analysis-viewer.events.maps :as events.maps]
            [analysis-viewer.fxs]
            [analysis-viewer.views :as views]
            [clojure.string :as str]
            [day8.re-frame.http-fx]
            [flow-storm.api :as fsa]
            [re-frame.core :as re-frame]
            [re-frame.db]
            [reagent.dom :as rdom]))

(defn ^:dev/before-load stop []
  (js/console.log "Stopping..."))

;; google-chrome --allow-file-access-from-files file:///home/jmonetta/non-rep-software/spread-d3/language_D3/renderers/d3/d3renderer/index.html

;; [CT] http://localhost:8021/?output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/5e55d631-99f8-4324-9ec9-386c9c4442d9.json&maps=
;; [CT] http://localhost:8021/?output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/3e18cc4c-6be8-46b7-b039-ec286f9e8e0d.json&maps=
;; [DT] http://localhost:8021/?output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/37f2cc7a-b3fc-4054-87d8-fd596cea22e2.json&maps=
;; [BF] http://localhost:8021/?output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/e4d05649-98d8-41af-a0e5-a0f1e5f63c26.json&maps=

(defn parse-url-qstring [qstring]
  (->> (str/split qstring #"&")
       (map (fn [s]
              (let [[pname pval] (str/split s #"=")]
                [(keyword pname) pval])))
       (into {})))

(defn ^:dev/after-load start []
  (js/console.log "Starting..." )
  #_(fsa/connect {:tap-name "analysis-viewer"})
  #_(fsa/trace-ref re-frame.db/app-db {:ref-name "re-frame-db"
                                     :ignore-keys [:maps/data ;; this one is super big
                                                   :map/state ;; this one changes a lot when zooming, dragging, etc
                                                   :animation/frame-timestamp ;; this one changes a lot
                                                   ]}) 
  (let [{:keys [maps output]} (parse-url-qstring (subs js/window.location.search 1))]
    (re-frame/dispatch-sync [:map/initialize
                             (into ["WORLD"] (remove str/blank? (str/split maps #",")) )                             
                             (str events.maps/s3-bucket-url output)])
    (rdom/render [views/main-screen]
                 (.getElementById js/document "app"))))

(defn ^:export init []
  (start))
