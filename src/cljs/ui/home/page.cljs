(ns ui.home.page
  (:require [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.box :refer [box]]
            [shared.components :refer [button]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.styles :as styles]
            [reagent.core :as reagent]
            [ui.component.app-container :refer [app-container]]
            [ui.component.icon :refer [arg->icon icons]]
            [ui.router.component :refer [page]]
            [ui.utils :as ui-utils :refer [>evt]]
            [shared.components :refer [spread-logo]]))

(defmethod page :route/home []
  (fn []
    [app-container
     [:div.home-page
      [:section.logo-section [spread-logo]]
      [:section.main-section
       [:h2 "Welcome to Spread."]
       [:p "System for visualizing phylogeographic reconstructions."]
       [:p "Click on the button below and follow the given directions."]
       [button {:text "Run new analysis"
                :icon "icons/icn_run_analysis_white.svg"
                :on-click #(>evt [:router/navigate :route/new-analysis nil {:tab "continuous-mcc-tree"}])
                :class "primary"}]]
      [:section.bottom-section
       [:p "Check documentation for further informations"]
       [button {:text "Read documentation"
                :on-click #(>evt [:router/navigate :route/documentation])
                :class "secondary"}]]]]))
