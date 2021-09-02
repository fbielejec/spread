(ns ui.analysis-results.discrete-rates
  (:require [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.typography :refer [typography]]
            [shared.components :refer [labeled-field]]))

(defn discrete-rates [{:keys [readable-name burn-in]}]
  [:div.field-table
   [:div.field-line
    [:div.field-card 
     [:h4 "Tree file"]
     [labeled-field {:label "Name" :text readable-name}]]]
   [:div.field-line
    [:div.field-card
     [:h4 "Burn in"]
     [labeled-field {:label "Burn in" :text burn-in}]]]])
