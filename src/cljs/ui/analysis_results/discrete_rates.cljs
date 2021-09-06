(ns ui.analysis-results.discrete-rates
  (:require [shared.components :refer [labeled-field]]))

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
