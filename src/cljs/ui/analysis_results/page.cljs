(ns ui.analysis-results.page
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.accordion :refer [accordion]]
            [reagent-material-ui.core.accordion-details :refer [accordion-details]]
            [reagent-material-ui.core.accordion-summary :refer [accordion-summary]]
            [reagent-material-ui.core.app-bar :refer [app-bar]]
            [reagent-material-ui.core.box :refer [box]]
            [shared.components :refer [button]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.tab :refer [tab]]
            [reagent-material-ui.core.table :refer [table]]
            [reagent-material-ui.core.table-body :refer [table-body]]
            [reagent-material-ui.core.table-cell :refer [table-cell]]
            [reagent-material-ui.core.table-container :refer [table-container]]
            [reagent-material-ui.core.table-head :refer [table-head]]
            [reagent-material-ui.core.table-row :refer [table-row]]
            [reagent-material-ui.core.tabs :refer [tabs]]
            [reagent-material-ui.core.toolbar :refer [toolbar]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.styles :as styles]
            [reagent.core :as reagent]
            [ui.analysis-results.continuous-mcc-tree :refer [continuous-mcc-tree]]
            [ui.analysis-results.discrete-mcc-tree :refer [discrete-mcc-tree]]
            [ui.analysis-results.discrete-rates :refer [discrete-rates]]
            [ui.component.app-container :refer [app-container]]
            [ui.component.icon :refer [icons]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :refer [<sub >evt]]
            [tick.alpha.api :as t]))


(defn data [{:keys [of-type status error] :as analysis} classes]
  [grid {:container true
         :direction :column}

   ;; error
   (when (= "ERROR" status)
     [accordion {:defaultExpanded true
                 :class-name      (:error-accordion classes)}
      [accordion-summary {:expand-icon (reagent/as-element [:img {:src (:dropdown icons)}])}
       [:div
        [:div {:class-name (:centered classes)}
         [:img {:src (:error icons)}]
         [typography {:class-name (:error-header classes)} "Error occured while running analysis"]]
        [typography {:class-name nil } "Check full report"]]]
      [accordion-details {:class-name (:details classes)}
       error]])

   ;; data
   (case of-type
     "CONTINUOUS_TREE"
     [continuous-mcc-tree analysis classes]
     "DISCRETE_TREE"
     [discrete-mcc-tree analysis classes]
     "BAYES_FACTOR_ANALYSIS"
     [discrete-rates analysis classes]
     nil)

   ;; buttons
   [box {:paddingTop    10
         :paddingBottom 10}
    [divider {:variant    "fullWidth"
              :class-name (:divider classes)}]]
   [grid {:container true
          :item      true
          :direction :row
          :xs        12 :xm 12}
    [grid {:item true
           :xs   4 :xm 4}
     [button {:variant   :contained
              :color     "primary"
              :size      :large
              :className (:start-button classes)
              :on-click  #(prn "TODO")}
      "Edit"]]
    [grid {:item true
           :xs   4 :xm 4}
     [button {:variant   :contained
              :color     "primary"
              :size      :large
              :className (:start-button classes)
              :on-click  #(prn "TODO")}
      "Copy settings"]]
    [grid {:item true
           :xs   4 :xm 4}
     [button {:variant   :contained
              :color     "primary"
              :size      :large
              :className (:start-button classes)
              :on-click  #(prn "TODO")}
      "Delete"]]]])




;; NOTE : the results tab
;; https://app.zeplin.io/project/6075ecb45aa2eb47e1384d0b/screen/6075ed3112972c3f62905120
(defn results [{:keys [bayes-factors] :as analysis} classes]  
  (let [config @(re-frame/subscribe [::subs/config])
        viewer-host (:analysis-viewer-url config)
        {:keys [viewer-url-params]} (:analysis analysis)
        viewer-url (str viewer-host "/" viewer-url-params)]
    [:div.results
     [:section.visualization
      [:h4 "Visualisations on a geographical map"]
      [:div
       [:span.link viewer-url]
       [button {:text "Copy"
                :on-click #(js/navigator.clipboard.writeText viewer-url)
                :class "golden"}]]]
     [:section.table
      (when bayes-factors
            [:div.export
             [:span "Support values"]
             [button {:text "Export To CSV"
                      :on-click #()
                      :class "secondary"}]]
            ;; TODO : table with sorting https://material-ui.com/components/tables/#sorting-amp-selecting
            [table-container {:class-name (:scroll-list classes)}
             [table {:class-name (:table classes)}
              [table-head
               [table-row
                [table-cell {:align :right} "From"]
                [table-cell {:align :right} "To"]
                [table-cell {:align :right} "Bayes Factor"]
                [table-cell {:align :right} "Posterior probability"]]]
              [table-body
               (doall
                (map (fn [{:keys [from to bayes-factor posterior-probability]}]
                       [table-row {:key (str from to)}
                        [table-cell {:align :right} from]
                        [table-cell {:align :right} to]
                        [table-cell {:align :right} bayes-factor]
                        [table-cell {:align :right} posterior-probability]])
                     bayes-factors))]]])]]))

(defn tab-pane [{:keys [id]}]
  (let [analysis (re-frame/subscribe [::subs/analysis-results id])]
    (fn [{:keys [id active-tab classes]}]
      [:<>
       [tabs {:value     active-tab
              :centered  true
              :classes   {:indicator (:indicator classes)}
              :on-change (fn [_ value]
                           (>evt [:router/navigate :route/analysis-results nil {:id id :tab value}]))}
        [tab {:value "data"
              :label "Data"}]
        [tab {:value "results"
              :label "Analysis results"}]]
       (case active-tab
         "data"    [data @analysis classes]
         "results" [results @analysis classes]
         [results @analysis classes])])))

(def type->label {"CONTINUOUS_TREE"       ["Continuous:" "MCC tree"]
                  "DISCRETE_TREE"         ["Discrete:" "MCC tree"]
                  "BAYES_FACTOR_ANALYSIS" ["Discrete:" "Bayes factor rates"]})

(defn analysis-header [{:keys [readable-name of-type created-on]}]
  (let [[label text] (type->label of-type)
        date (time/format-date-str created-on) 
        time (time/format-time-str created-on)]
    [:div.analysis-header {:style {:grid-area "header"}}
     [:div.readable-name {:style {:grid-area "readable-name"}} readable-name]
     [:div.sub-name {:style {:grid-area "sub-name"}}
      [:span.label label] [:span.text text]]
     [:div.datetime {:style {:grid-area "datetime"}}
      [:span.date date] [:span.time time]]]))

(defn analysis-body [active-page]  
  (let [{{:keys [id] :as query} :query} active-page            
        active-tab (or (:tab query) "results")]
    [:div.body {:style {:grid-area "body"}}     
     [tab-pane {:id         id                
                :active-tab active-tab}]]))

(defn footer []
  [:div.footer-wrapper
   [:div.footer {:style {:grid-area "footer"}}
    [button {:text "Edit"
             :on-click #()
             :class "golden"}]
    [button {:text "Copy settings"
             :on-click #()
             :class "secondary"}]
    [button {:text "Delete"
             :on-click #()
             :class "danger"}]]])

(defmethod page :route/analysis-results []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn []
      (let [{{:keys [id] :as query} :query}                         @active-page
            {:keys [readable-name of-type created-on] :as analysis} (<sub [::subs/analysis-results id])
            active-tab                                              (or (:tab query) "results")]
        (if-not readable-name
          [:div "Loading..." ]
          [app-container
           [:div.analysis-results
            [analysis-header analysis]
            [analysis-body @active-page]
            [footer]]
           #_[grid
            [app-bar {:position   "static"
                      :color      :transparent
                      :class-name (:app-bar classes)}
             [toolbar {:class-name (:centered classes)}
              ;; main
              [grid {:container     true
                     :direction     :column
                     :align-items   :center
                     :align-content :center}
               ;; gutter
               [grid {:item true :xs 2 :xm 2}]
               ;; row
               [grid {:item true :xs 8 :xm 8}
                [typography {:class-name (:header classes)} readable-name]
                ;; row
                [grid {:item      true
                       :container true
                       :spacing   10}
                 [grid {:item true}
                  (case of-type
                    "CONTINUOUS_TREE"       [box {:class-name (:box classes)}
                                             [typography "Continuous:"]
                                             [typography "MCC tree"]]
                    "DISCRETE_TREE"         [box {:class-name (:box classes)}
                                             [typography "Discrete:"]
                                             [typography "MCC tree"]]
                    "BAYES_FACTOR_ANALYSIS" [box {:class-name (:box classes)}
                                             [typography "Discrete:"]
                                             [typography "Bayes factor rates"]]
                    nil)]
                 [grid {:item true}
                  [typography  (when created-on
                                 (-> created-on time/string->date (time/format true)))]]]]
               ;; gutter
               [grid {:item true :xs 2 :xm 2}]]]]
            [tab-pane {:id         id
                       ;; :analysis   analysis
                       :active-tab active-tab
                       :classes    classes}]]])))))
