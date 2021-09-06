(ns ui.new-analysis.discrete-mcc-tree
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

(defn controls [{:keys [id readable-name locations-attribute locations-file-url most-recent-sampling-date time-scale-multiplier]} {:keys [disabled?]}]
  [:div.controls-wrapper
   [:div.controls {:style {:grid-area "controls"}}
    [button {:text "Start analysis"
             :on-click #(dispatch-n [[:discrete-mcc-tree/start-analysis {:readable-name             readable-name
                                                                         :locations-attribute-name  locations-attribute
                                                                         :locations-file-url        locations-file-url
                                                                         :most-recent-sampling-date most-recent-sampling-date
                                                                         :time-scale-multiplier     time-scale-multiplier}]
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
             :on-click #(js/alert "Yeahhhhhhh")
             :class "danger"
             :disabled? disabled?}]]])

(defn discrete-mcc-tree [classes]
  (let [discrete-mcc-tree (re-frame/subscribe [::subs/discrete-mcc-tree])
        field-errors      (re-frame/subscribe [::subs/discrete-mcc-tree-field-errors])]
    (fn []
      (let [{:keys [id
                    tree-file tree-file-upload-progress
                    locations-file locations-file-url
                    ;; locations-file-upload-progress
                    readable-name
                    locations-attribute
                    most-recent-sampling-date
                    time-scale-multiplier
                    attribute-names]
             :or   {locations-attribute       (first attribute-names)
                    most-recent-sampling-date (time/now)
                    time-scale-multiplier     1}
             :as analysis}
            @discrete-mcc-tree
            controls-disabled? (or (not attribute-names) (not locations-file))]
        [:div.run-new-discrete
         [:div.data {:style {:grid-area "data"}}
          [:section.load-tree-file
           [:div
            [:h4 "Load tree file"]
            (cond
              (and (nil? tree-file-upload-progress) (nil? tree-file))            
              [button-file-upload {:id               "discrete-mcc-tree-file-upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:discrete-mcc-tree/on-tree-file-selected %])}]

              (not= 1 tree-file-upload-progress)
              [linear-progress {:value      (* 100 tree-file-upload-progress)
                                :variant    "determinate"}]

              tree-file
              [loaded-input {:value    tree-file
                             :on-click #(>evt [:discrete-mcc-tree/delete-tree-file])}]

              :else nil)]
           (when (nil? tree-file)
             [:p.doc "When upload is complete all unique attributes will be automatically filled. You can then select location attribute and change other settings."])]
          [:section.load-locations-file
           [:div
            [:h4 "Load locations file"]
            [button-file-upload {:id               "discrete-mcc-locations-file-upload-button"
                                 :label            "Choose a file"
                                 :on-file-accepted #(>evt [:discrete-mcc-tree/on-locations-file-selected %])}]]
           (when (nil? locations-file)
             [:p.doc "Select a file that maps geographical coordinates to the location attribute states. Once this file is uploaded you can start your analysis."])]

          [:div.upload-spinner
           (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
             [circular-progress {:size 100}])]

          (when (and attribute-names locations-file)
            [:div.field-table
             [:div.field-line             
              [:div.field-card 
               [:h4 "File info"]
               [text-field {:label     "Name" :variant :outlined
                            :value     readable-name                           
                            :on-change (fn [_ value] (>evt [:discrete-mcc-tree/set-readable-name value]))}]]
              [:div.field-card
               [:h4 "Select location attributes"]
               [attributes-select {:classes   classes
                                   :id        "select-locations-attribute"
                                   :value     locations-attribute
                                   :options   attribute-names
                                   :label     "Locations attribute"
                                   :on-change (fn [value]
                                                (>evt [:discrete-mcc-tree/set-locations-attribute value]))}]]]
             [:div.field-line             
              [:div.field-card
               [:h4 "Most recent sampling date"]
               [date-picker {:wrapperClassName (:date-picker classes)
                             :date-format      time/date-format
                             :on-change        #(>evt [:discrete-mcc-tree/set-most-recent-sampling-date %])
                             :selected         most-recent-sampling-date}]]
              [:div.field-card
               [:h4 "Time scale"]
               [amount-input {:label       "Multiplier"
                              :value       time-scale-multiplier
                              :error?      (not (nil? (:time-scale-multiplier @field-errors)))
                              :helper-text (:time-scale-multiplier @field-errors)
                              :on-change   (fn [value]
                                             (>evt [:discrete-mcc-tree/set-time-scale-multiplier value]))}]]]])]
         [controls analysis {:disabled? controls-disabled? }]]))))
