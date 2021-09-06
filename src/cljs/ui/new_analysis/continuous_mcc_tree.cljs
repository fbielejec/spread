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
            controls-disabled? (or (not attribute-names) (not trees-file))]
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
                                 :on-file-accepted #(>evt [:continuous-mcc-tree/on-tree-file-selected %])}]]
           (when (nil? trees-file)
             [:p.doc "Optional: Select a file with corresponding trees distribution. This file will be used to compute a density interval around the MCC tree."])]

          [:div.upload-spinner
           (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
             [circular-progress {:size 100}])]

          (when (and attribute-names trees-file)
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

#_(defn continuous-mcc-tree [classes]
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
                    time-scale-multiplier     1}}
            @continuous-mcc-tree]
        ;; main container
        #_[grid {:container true
               :direction :column
               :spacing   1}

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}
          [grid {:item true
                 :xs   6 :xm 6}
           [typography {:class-name (:input-label classes)} "Load tree file"]]
          [grid {:item true :xs 6 :xm 6}]]

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}
          ;; column left
          [grid {:item true :xs 6 :xm 6}
           (cond
             (and (nil? tree-file-upload-progress) (nil? tree-file))
             [button-file-upload {:id               "continuous-mcc-tree-file-upload-button"
                                  :label            "Choose a file"
                                  :class-name       (:upload-button classes)
                                  :icon             :upload
                                  :on-file-accepted #(>evt [:continuous-mcc-tree/on-tree-file-selected %])}]

             (not= 1 tree-file-upload-progress)
             [linear-progress {:value      (* 100 tree-file-upload-progress)
                               :variant    "determinate"
                               :class-name (:upload-progress classes)}]

             tree-file
             [loaded-input {:classes  classes
                            :value    tree-file
                            :on-click #(>evt [:continuous-mcc-tree/delete-tree-file])}]

             :else nil)]
          ;; column right
          [grid {:item true :xs 6 :xm 6}
           (when (nil? tree-file)
             [:<>
              [typography "When upload is complete all unique attributes will be automatically filled."]
              [typography "You can then select geographical coordinates and change other settings."]])]]

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}
          ;; col left
          [grid {:item true :xs 6 :xm 6}
           [typography {:class-name (:input-label classes)} "Load trees file"]]
          ;; col right
          [grid {:item true :xs 6 :xm 6}]]

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}
          ;; col left
          [grid {:item true :xs 6 :xm 6}
           (cond
             (and (nil? trees-file-upload-progress) (nil? trees-file))
             [button-file-upload {:id               "mcc-trees-file-upload-button"
                                  :disabled?        (nil? attribute-names)
                                  :label            "Choose a file"
                                  :class-name       (:upload-button classes)
                                  :icon             :upload
                                  :on-file-accepted #(>evt [:continuous-mcc-tree/on-tree-file-selected %])}]

             (not= 1 trees-file-upload-progress)
             [linear-progress {:value      (* 100 trees-file-upload-progress)
                               :variant    "determinate"
                               :class-name (:upload-progress classes)}]

             trees-file
             [loaded-input {:classes  classes
                            :value    trees-file
                            :on-click #(>evt [:continuous-mcc-tree/delete-trees-file])}]

             :else nil)]

          ;; col right
          [grid {:item true
                 :xs   6 :xm 6}
           (when (nil? trees-file)
             [:<>
              [typography "Optional: Select a file with corresponding trees distribution."]
              [typography "This file will be used to compute a density interval around the MCC tree."]])]]

         ;; row
         [grid {:container     true
                :item          true
                :direction     :column
                :xs            12 :xm 12
                :align-items   :center
                :align-content :center}
          (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
            [circular-progress {:size 100}])]

         (when attribute-names
           [:<>
            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true :xs 6 :xm 6}
              [text-field {:label     "Name" :variant :outlined
                           :value     readable-name
                           ;; :shrink    (nil? readable-name)
                           :on-change (fn [_ value] (>evt [:continuous-mcc-tree/set-readable-name value]))}]]
             ;; col right
             [grid {:item true :xs 6 :xm 6}]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true :xs 6 :xm 6}
              [typography {:class-name (:input-label classes)} "Select y coordinate"]]

             ;; col right
             [grid {:item true :xs 6 :xm 6}
              [typography {:class-name (:input-label classes)} "Select x coordinate"]]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true :xs 6 :xm 6}
              [attributes-select {:classes   classes
                                  :id        "select-latitude"
                                  :value     y-coordinate
                                  :options   attribute-names
                                  :label     "Latitude"
                                  :on-change (fn [value]
                                               (>evt [:continuous-mcc-tree/set-y-coordinate value]))}]]
             ;; col right
             [grid {:item true :xs 6 :xm 6}
              [attributes-select {:classes   classes
                                  :id        "select-longitude"
                                  :value     x-coordinate
                                  :options   attribute-names
                                  :label     "Longitude"
                                  :on-change (fn [value]
                                               (>evt [:continuous-mcc-tree/set-x-coordinate value]))}]]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true :xs 6 :xm 6}
              [typography {:class-name (:input-label classes)} "Most recent sampling date"]]
             ;; col right
             [grid {:item true :xs 6 :xm 6}
              [typography {:class-name (:input-label classes)} "Time scale"]]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true
                    :xs   6 :xm 6}
              [date-picker {:wrapperClassName (:date-picker classes)
                            :date-format      time/date-format
                            :on-change        #(>evt [:continuous-mcc-tree/set-most-recent-sampling-date %])
                            :selected         most-recent-sampling-date}]]
             ;; col right
             [grid {:item true
                    :xs   6 :xm 6}
              [amount-input {:label       "Multiplier"
                             :value       time-scale-multiplier
                             :error?      (not (nil? (:time-scale-multiplier @field-errors)))
                             :helper-text (:time-scale-multiplier @field-errors)
                             :on-change   (fn [value]
                                            (>evt [:continuous-mcc-tree/set-time-scale-multiplier value]))}]]]

            [box {:padding-top    5
                  :padding-bottom 5}
             [divider {:variant "fullWidth"}]]

            [grid {:container true
                   :direction :row
                   :spacing   1}
             [grid {:item true}
              [button {:variant   "contained"
                       :color     "primary"
                       :size      "large"
                       :disabled  (boolean (seq @field-errors))
                       :className (:start-button classes)
                       :on-click  #(dispatch-n [[:continuous-mcc-tree/start-analysis {:readable-name             readable-name
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
                                                                        :variables {"id" id}}]])}
               "Start analysis"]]

             [grid {:item true}
              [button {:variant   "contained"
                       :color     "primary"
                       :size      "large"
                       :className (:start-button classes)
                       :on-click  #(prn "TODO")}
               "Paste settings"]]

             [grid {:item true}
              [button {:variant   "contained"
                       :color     "primary"
                       :size      "large"
                       :className (:start-button classes)
                       :on-click  #(prn "TODO")}
               "Reset"]]]])]))))
