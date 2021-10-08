(ns ui.new-analysis.discrete-rates
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.circular-progress :refer [circular-progress]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent-material-ui.core.slider :refer [slider]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [shared.components :refer [button]]
            [ui.component.button :refer [button-file-upload]]
            [ui.component.input :refer [loaded-input]]
            [ui.subscriptions :as subs]
            [ui.utils :as ui-utils :refer [>evt dispatch-n]]))

(defn controls [{:keys [id readable-name burn-in locations-file-url]} {:keys [disabled?]}]
  [:div.controls-wrapper
   [:div.controls {:style {:grid-area "controls"}}
    [button {:text "Start analysis"
             :on-click #(dispatch-n [[:bayes-factor/start-analysis {:readable-name      readable-name
                                                                    :burn-in            (/ burn-in 100)
                                                                    :locations-file-url locations-file-url}]
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

(defn discrete-rates []
  (let [bayes-factor (re-frame/subscribe [::subs/bayes-factor])
        field-errors (re-frame/subscribe [::subs/bayes-factor-field-errors])]
    (fn []
      (let [{:keys [id
                    locations-file-url
                    log-file
                    log-file-upload-progress
                    locations-file
                    locations-file-upload-progress
                    readable-name
                    burn-in]
             :or   {burn-in 0.1}}
            @bayes-factor
            controls-disabled? (or @field-errors (not log-file))]
        [:<>
         [:div.data {:style {:grid-area "data"}}
          [:section.load-log-file
           [:div
            [:h4 "Load log file"]
            (cond
              (and (nil? log-file-upload-progress) (nil? log-file))
              [button-file-upload {:id               "bayes-factor-log-file-upload-button"
                                   :label            "Choose a file"
                                  :on-file-accepted #(>evt [:bayes-factor/on-log-file-selected %])}]

              (not= 1 log-file-upload-progress)
              [linear-progress {:value      (* 100 log-file-upload-progress)
                                :variant    "determinate"}]

              log-file
              [loaded-input {:value    log-file
                             :on-click #(>evt [:bayes-factor/delete-log-file])}]

              :else nil)]
           (when (nil? log-file)
             [:p.doc "Upload log file. You can then upload a matching coordinates file."])]
          [:section.load-locations-file
           [:div
            [:h4 "Load locations file"]
            (cond
              (nil? locations-file)
              [button-file-upload {:id               "bayes-factor-locations-file-upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:bayes-factor/on-locations-file-selected %])}]

              (not= 1 locations-file-upload-progress)
              [linear-progress {:value   (* 100 locations-file-upload-progress)
                                :variant "determinate"}]

              locations-file
              [loaded-input {:value    locations-file
                             :on-click #(>evt [:bayes-factor/delete-locations-file])}]

              :else nil)]
           (when (nil? locations-file)
             [:p.doc "Select a file that maps geographical coordinates to the log file columns. Once this file is uploaded you can start your analysis."])]

          (when (and (= 1 log-file-upload-progress)
                     (= 1 locations-file-upload-progress))
            [:div.field-table
             [:div.field-line
              [:div.field-card
               [:h4 "File info"]
               [text-field {:label     "Name" :variant :outlined
                            :value     readable-name
                            :on-change (fn [_ value] (>evt [:bayes-factor/set-readable-name value]))}]]
              [:div.field-card
               [:h4 "Select burn-in"]
               [slider {:value             burn-in
                       :min               0.0
                       :max               0.9
                       :step              0.1
                       :valueLabelDisplay :auto
                        :marks             true
                       :on-change         (fn [_ value]
                                            (>evt [:bayes-factor/set-burn-in value]))}]]]])]
         [controls {:id id
                    :readable-name readable-name
                    :burn-in burn-in
                    :locations-file-url locations-file-url}
          {:disabled? controls-disabled?}]]))))
