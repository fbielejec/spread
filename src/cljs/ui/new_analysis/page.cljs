(ns ui.new-analysis.page
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.app-bar :refer [app-bar]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.tab :refer [tab]]
            [reagent-material-ui.core.tabs :refer [tabs]]
            [reagent-material-ui.core.toolbar :refer [toolbar]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.styles :as styles]
            [reagent.core :as reagent]
            [ui.component.app-container :refer [app-container]]
            [ui.new-analysis.continuous-mcc-tree :refer [continuous-mcc-tree]]
            [ui.new-analysis.discrete-mcc-tree :refer [discrete-mcc-tree]]
            [ui.new-analysis.discrete-rates :refer [discrete-rates]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.utils :as ui-utils :refer [>evt]]
            [shared.components :refer [button]]))

(defn header []
  [:div.header
   [:h2 "Run new analysis"]])

(defn body [active-tab]
  [:div.body
   [tabs {:value     active-tab
          :centered  true
          :on-change (fn [_ value]
                       (>evt [:router/navigate :route/new-analysis nil {:tab value}]))}
    [tab {:value "discrete-mcc-tree"
          :label (reagent/as-element
                  [:div
                   [typography {} "Discrete"]
                   [typography {} "MCC tree"]])}]
    [tab {:value "discrete-rates"
          :label (reagent/as-element
                  [:div
                   [typography {} "Discrete"]
                   [typography {} "Rates"]])}]
    [tab {:value "continuous-mcc-tree"
          :label (reagent/as-element
                  [:div
                   [typography {} "Continuous"]
                   [typography {} "MCC tree"]])}]]
   (case active-tab
     "discrete-mcc-tree"   [discrete-mcc-tree {}]
     "discrete-rates"      [discrete-rates {}]
     "continuous-mcc-tree" [continuous-mcc-tree {}])])


;; NOTE: specs
;; https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/db6d1f78-c5f4-460e-9f97-32e9df007388/specs/
;; https://app.zeplin.io/project/6075ecb45aa2eb47e1384d0b/screen/6075ed2b39f8103c14a9c660
(defmethod page :route/new-analysis []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn []
      (let [{:keys [query]}   @active-page
            {active-tab :tab} query]
        [app-container
         [:div.run-new-analysis
          [header]
          [body active-tab]]]))))
