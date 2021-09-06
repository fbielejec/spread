(ns ui.new-analysis.continuous-mcc-tree
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.box :refer [box]]
            [shared.components :refer [button]]
            [reagent-material-ui.core.circular-progress :refer [circular-progress]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.typography :refer [typography]]
            [ui.component.button :refer [button-file-upload]]
            [ui.component.date-picker :refer [date-picker]]
            [ui.component.input :refer [amount-input loaded-input]]
            [ui.component.select :refer [attributes-select]]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt dispatch-n]]))

(defn controls [{:keys [id readable-name y-coordinate x-coordinate most-recent-sampling-date time-scale-multiplier]} {:keys [disabled?]}]
  [:div.controls-wrapper
   [:div.controls {:style {:grid-area "controls"}}
    [button {:text "Start analysis"
             :on-click #(dispatch-n [[:continuous-mcc-tree/start-analysis {:readable-name             readable-name
                                                                           :y-coordinate              y-coordinate
                                                                           :x-coordinate              x-coordinate
                                                                           :most-recent-sampling-date most-recent-sampling-date
                                                                           :time-scale-multiplier     time-scale-multiplier}]
                                     ;; NOTE : normally we have a running subscription already, but in case the user re-starts the analysis here we dispatch it again.
                                     ;; it is de-duplicated by the id anyway
                                     [:graphql/subscription {:id        id
                                                             :query     "subscription SubscriptionRoot($id: ID!) {
                                                                                                parserStatus(id: $id) {
                                                                                                  id
                                                                                                  status
                                                                                                  progress
                                                                                                  ofType
                                                                                                }}"
                                                             :variables {"id" id}}]])
             :class "golden"
             :disabled? disabled?}]
    [button {:text "Paste settings"
             :on-click #()
             :class "secondary"
             :disabled? disabled?}]
    [button {:text "Reset"
             :on-click #()
             :class "danger"
             :disabled? disabled?}]]])

(defn continuous-mcc-tree [classes]
  (let [continuous-mcc-tree (re-frame/subscribe [::subs/continuous-mcc-tree])
        field-errors        (re-frame/subscribe [::subs/continuous-mcc-tree-field-errors])]
    (fn []
      (let [{:keys [id
                    readable-name
                    tree-file tree-file-upload-progress
                    trees-file trees-file-upload-progress
                    y-coordinate x-coordinate
                    most-recent-sampling-date
                    time-scale-multiplier
                    attribute-names]
             :or   {y-coordinate              (first attribute-names)
                    x-coordinate              (first attribute-names)
                    most-recent-sampling-date (time/now)
                    time-scale-multiplier     1}
             :as analysis}
            @continuous-mcc-tree
            controls-disabled? (or (not attribute-names) (not tree-file))]
        [:div.run-new-continuous
         [:div.data {:style {:grid-area "data"}}
          [:section.load-tree-file
           [:div
            [:h4 "Load tree file"]
            (cond
              (and (nil? tree-file-upload-progress) (nil? tree-file))            
              [button-file-upload {:id               "continuous-mcc-tree-file-upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:continuous-mcc-tree/on-tree-file-selected %])}]

              (not= 1 tree-file-upload-progress)
              [linear-progress {:value      (* 100 tree-file-upload-progress)
                                :variant    "determinate"}]

              tree-file
              [loaded-input {:value    tree-file
                             :on-click #(>evt [:continuous-mcc-tree/delete-tree-file])}]

              :else nil)]
           (when (nil? tree-file)
             [:p.doc "When upload is complete all unique attributes will be automatically filled.You can then select geographical coordinates and change other settings."])]
          
          [:section.load-trees-file
           [:div
            [:h4 "Load trees file"]
            [button-file-upload {:id               "mcc-trees-file-upload-button"
                                 :label            "Choose a file"
                                 :on-file-accepted #(>evt [:continuous-mcc-tree/on-trees-file-selected %])}]]
           (when (nil? trees-file)
             [:p.doc "Optional: Select a file with corresponding trees distribution. This file will be used to compute a density interval around the MCC tree."])]

          [:div.upload-spinner
           (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
             [circular-progress {:size 100}])]

          (when (and attribute-names tree-file)
            [:div.field-table
             [:div.field-line             
              [:div.field-card 
               [:h4 "File info"]
               [text-field {:label     "Name" 
                            :value     readable-name                           
                            :on-change (fn [_ value] (>evt [:continuous-mcc-tree/set-readable-name value]))}]]]
             [:div.field-line             
              [:div.field-card 
               [:h4 "Select x coordinate"]
               [attributes-select {:classes   classes
                                   :id        "select-longitude"
                                   :value     x-coordinate
                                   :options   attribute-names
                                   :label     "Longitude"
                                   :on-change (fn [value]
                                                (>evt [:continuous-mcc-tree/set-x-coordinate value]))}]]
              [:div.field-card 
               [:h4 "Select y coordinate"]
               [attributes-select {:classes   classes
                                   :id        "select-latitude"
                                   :value     y-coordinate
                                   :options   attribute-names
                                   :label     "Latitude"
                                   :on-change (fn [value]
                                                (>evt [:continuous-mcc-tree/set-y-coordinate value]))}]]]
             [:div.field-line             
              [:div.field-card
               [:h4 "Most recent sampling date"]
               [date-picker {:wrapperClassName (:date-picker classes)
                             :date-format      time/date-format
                             :on-change        #(>evt [:continuous-mcc-tree/set-most-recent-sampling-date %])
                             :selected         most-recent-sampling-date}]]
              [:div.field-card
               [:h4 "Time scale"]
               [amount-input {:label       "Multiplier"
                              :value       time-scale-multiplier
                              :error?      (not (nil? (:time-scale-multiplier @field-errors)))
                              :helper-text (:time-scale-multiplier @field-errors)
                              :on-change   (fn [value]
                                             (>evt [:continuous-mcc-tree/set-time-scale-multiplier value]))}]]]])]
         [controls analysis {:disabled? controls-disabled? }]]))))
