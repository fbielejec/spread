(ns ui.component.app-container
  (:require ["react" :as react]
            [re-frame.core :as re-frame]
            [reagent-material-ui.core.icon-button :refer [icon-button]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent-material-ui.core.menu :refer [menu]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent.core :as reagent]
            [shared.components :refer [collapsible-tab spread-logo button]]
            [ui.component.icon :refer [icons]]
            [ui.component.search :refer [search-bar]]
            [ui.format :refer [format-percentage]]
            [ui.subscriptions :as subs]
            [ui.utils :as ui-utils :refer [>evt dispatch-n]]))

(def type->label {"CONTINUOUS_TREE"       "Continuous: MCC tree"
                  "DISCRETE_TREE"         "Discrete: MCC tree"
                  "BAYES_FACTOR_ANALYSIS" "Discrete: Bayes Factor Rates"})

(defn completed-menu-item [_]
  (let [menu-open? (reagent/atom false)]
    (fn [{:keys [id readable-name of-type status new?]}]      
      (let [badge-text (cond
                         (= status "ERROR") "Error"
                         new?               "New")]
        [:div.completed-menu-item.clickable {:on-click #(dispatch-n [[:router/navigate :route/analysis-results nil {:id id :tab "results"}]
                                                           (when new?
                                                             [:graphql/query {:query
                                                                              "mutation TouchAnalysisMutation($analysisId: ID!) {
                                                             touchAnalysis(id: $analysisId) {
                                                               id
                                                               isNew
                                                             }
                                                           }"
                                                                              :variables {:analysisId id}}])])}
         [:div.readable-name {:style {:grid-area "readable-name"}} (or readable-name "Unknown")]
         [:div.badges (when badge-text [:span.badge badge-text])]
         [:div.sub-name {:style {:grid-area "sub-name"}} (type->label of-type)]
         [:div {:style {:grid-area "menu"}
                :on-click #(swap! menu-open? not)}
          [:img {:src "icons/icn_kebab_menu.svg"}]]
         (when @menu-open?
           [:ul.menu
            [:li {:on-click #()} "Edit"]
            [:li {:on-click #()} "Load different file"]
            [:li {:on-click #()} "Copy settings"]
            [:li {:on-click #()} "Delete"]])]))))

(defn completed []
  (let [search-term        (re-frame/subscribe [::subs/search])
        completed-analysis (re-frame/subscribe [::subs/completed-analysis-search])
        new-completed      (re-frame/subscribe [::subs/new-completed-analysis])]
    (fn []
      (let [items @completed-analysis
            new-count (count @new-completed)]
        [collapsible-tab (cond-> {:id :completed
                                  :title "Completed data analysis"
                                  :icon "icons/icn_previous_analysis.svg"                                  
                                  :child [:div.completed
                                          [search-bar {:value       (or "" @search-term)
                                                       :on-change   #(>evt [:general/set-search %])
                                                       :placeholder "Search"}]
                                          (for [{:keys [id] :as item} items]
                                            ^{:key id}
                                            [completed-menu-item (-> item
                                                                     ;; TODO : for dev
                                                                     #_(assoc :new? true)
                                                                     #_(assoc :status "ERROR")
                                                                     )
                                             {}])
                                          ]}
                           (> new-count 0) (assoc :badge-text (str new-count " New")
                                                  :badge-color "purple"))]))))

(defn queued-menu-item [{:keys [readable-name of-type progress]}]
  ;; TODO:
  ;;     - implement pause/resume button
  ;;     - minutes left
  ;;     - delete button
  ;;     - right top menu
  [:div.queued-menu-item
   [:div.readable-name {:style {:grid-area "readable-name"}} (or readable-name "Unknown")]
   [:div.sub-name {:style {:grid-area "sub-name"}} (type->label of-type)]
   [:div {:style {:grid-area "menu"}} [:img {:src "icons/icn_kebab_menu.svg"}]]
   [:div {:style {:grid-area "play-pause"}} [:img {:src "icons/pause.svg" #_"icons/pause.svg"}]]
   [:div {:style {:grid-area "delete"}} [:img {:src "icons/icn_delete.svg"}]]
   [:div.progress {:style {:grid-area "progress"}}
    [linear-progress {:value      (* 100 progress)
                      :variant    "determinate"}]
    [:span.finished (str (format-percentage progress 1.0) " finished")]]])

(defn queue []
  (let [queued-analysis (re-frame/subscribe [::subs/queued-analysis])]
    (fn []      
      (let [items @queued-analysis
            queued-count (count items)]
        [collapsible-tab (cond-> {:id :queued
                                  :title "Queued"
                                  :icon "icons/icn_queue.svg"                                
                                  :child [:div.queued
                                          (map (fn [{:keys [id] :as item}]
                                                 ^{:key id} [queued-menu-item item])
                                               items)]}
                           (> queued-count 0) (assoc :badge-text (str queued-count " Ongoing")
                                                     :badge-color "grey"))]))))

(defn run-new []
  (let [items [{:main-label "Discrete:"         :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "discrete-mcc-tree"}}
               {:main-label "Discrete:"         :sub-label "Bayes factor rates"
                :target     :route/new-analysis :query     {:tab "discrete-rates"}}
               {:main-label "Continuous:"       :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "continuous-mcc-tree"}}]]

    [collapsible-tab {:id :run-new
                      :title "Run new analysis"
                      :icon "icons/icn_run_analysis.svg"
                      :child [:div.run-new
                              (map-indexed (fn [index {:keys [main-label sub-label target query]}]
                                             [:li.clickable {:key      index
                                                             :button   true
                                                             :on-click #(>evt [:router/navigate target nil query])}
                                              [:span.label main-label]
                                              [:span.text sub-label]])
                                           items)]}]))

(defn main-menu []
  [:div.app-sidebar.panel
   [:div.collapsible-tabs
    [run-new]
    [completed]
    [queue]]
   [:div.footer
    [button {:text "Run new analysis"
             :on-click #(>evt [:router/navigate :route/new-analysis nil {:tab "continuous-mcc-tree"}])
             :class "primary"
             :icon "icons/icn_run_analysis_white.svg"}]]])

(defn user-login [classes]
  (let [{:keys [email]}                  @(re-frame/subscribe [::subs/authorized-user])
        [anchorElement setAnchorElement] (react/useState nil)
        handle-close                     #(setAnchorElement nil)
        open?                            (not (nil? anchorElement))]
    [:div
     [icon-button {:aria-label    "authed user menu"
                   :aria-controls "menu-appbar"
                   :aria-haspopup true
                   :color         "inherit"
                   :onClick       (fn [^js event]
                                    (setAnchorElement (.-currentTarget event)))}
      [typography {:class-name (:email classes)} email]
      [:img {:src (:user icons)}]
      [:img {:src (:dropdown icons)}]]
     [menu {:id               "menu-appbar"
            :anchorEl         anchorElement
            :anchorOrigin     {:vertical   "top"
                               :horizontal "right"}
            :keep-mounted     true
            :transform-origin {:vertical   "top"
                               :horizontal "right"}
            :open             open?
            :on-close         handle-close}
      [menu-item {:on-click (fn []
                              (handle-close)
                              (>evt [:general/logout]))} "Log out"]
      [menu-item {:on-click (fn []
                              (prn "TODO")
                              (handle-close))}
       "Clear data"]
      [menu-item {:on-click (fn []
                              (prn "TODO")
                              (handle-close))}
       "Delete account"]]]))

(defn header-logo []
  [:div.app-header-logo {:on-click #(>evt [:router/navigate :route/home])}
   [spread-logo]])

(defn header-menu []
  [:div.app-header-menu
   [user-login {}]])

(defn app-container []
  (fn [child-page]
    [:div.app-container-grid
     [:div.app-header-spacer-1]
     [header-logo]
     [header-menu]
     [:div.app-header-spacer-2]
     [main-menu]     
     [:div.app-body.panel
      child-page]]))
