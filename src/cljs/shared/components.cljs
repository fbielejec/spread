(ns shared.components
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn collapsible-tab [{:keys [id title icon badge-text badge-color child]}]
  (let [open? @(subscribe [:collapsible-tabs/open? id])]
    [:div.tab 
     [:div.title {:on-click #(dispatch [:collapsible-tabs/toggle id])}      
      [:span.text
       [:img {:src icon}]
       title]
      (when (and badge-text badge-color)
        [:span.badge {:style {:color badge-color
                              :border-color badge-color}}
         badge-text])
      [:span.arrow (if open? "▲" "▼")]]
     [:div.tab-body {:class (if open? "open" "collapsed")}
      child]]))

(defn spread-logo []
  [:div.spread-logo 
   [:div.logo-img
    [:div.hex.hex1] [:div.hex.hex2] [:div.hex.hex3] [:div.hex.hex4]]
   [:span.text "spread"]])
