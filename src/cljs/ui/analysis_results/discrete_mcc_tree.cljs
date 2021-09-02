(ns ui.analysis-results.discrete-mcc-tree
  (:require [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.typography :refer [typography]]
            [shared.components :refer [labeled-field]]))

(defn discrete-mcc-tree [{:keys [readable-name locations-attribute-name
                                 most-recent-sampling-date timescale-multiplier]}]
  [:div.field-table
   [:div.field-line
    [:div.field-card 
     [:h4 "Tree file"]
     [labeled-field {:label "Name" :text readable-name}]]]
   [:div.field-line
    [:div.field-card
     [:h4 "Time scale"]
     [labeled-field {:label "Multiplier" :text timescale-multiplier}]]
    [:div.field-card
     [:h4 "Locations"]
     [labeled-field {:label "Attribute name" :text locations-attribute-name}]]]
   [:div.field-line
    [:div.field-card
     [:h4 "Most recent sampling date"]
     [labeled-field {:label "YYYY/MM/DD" :text most-recent-sampling-date}]]]])
