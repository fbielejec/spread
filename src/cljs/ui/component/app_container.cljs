(ns ui.component.app-container
  (:require ["react" :as react]
            [re-frame.core :as re-frame]
            [reagent-material-ui.core.accordion #_:refer #_[accordion]]
            [reagent-material-ui.core.accordion-details :refer [accordion-details]]
            [reagent-material-ui.core.accordion-summary :refer [accordion-summary]]
            [reagent-material-ui.core.app-bar :refer [app-bar]]
            [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.box :refer [box]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.chip :refer [chip]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.icon-button :refer [icon-button]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]
            [reagent-material-ui.core.menu :refer [menu]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.toolbar :refer [toolbar]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.icons.search :refer [search]]
            [reagent-material-ui.styles :as styles]
            [reagent.core :as reagent]
            [ui.component.icon :refer [arg->icon icons]]
            [ui.component.search :refer [search-bar]]
            [ui.format :refer [format-percentage]]
            [ui.subscriptions :as subs]
            [ui.utils :as ui-utils :refer [>evt dispatch-n]]))

(defn accordion [m & childs]  
  [reagent-material-ui.core.accordion/accordion
   (assoc m :style {:margin "0px"})
   childs])

(def use-styles (styles/make-styles (fn [_]
                                      {:root {:background "#ECEFF8 0% 0% no-repeat padding-box"
                                              :min-width  "100%"
                                              :min-height "100vh"}

                                       :card {:box-shadow    "0px 30px 60px #313B5833"
                                              :border-radius "20px"}

                                       :centered {:display         "flex"
                                                  :flex-direction  "row"
                                                  :justify-content "center"}

                                       :email {:font           "normal normal medium 16px/19px Roboto"
                                               :letter-spacing "0px"
                                               :color          "#3A3668"}

                                       :app-bar {:background    "#FFFFFF 0% 0% no-repeat padding-box"
                                                 :box-shadow    :none
                                                 :border-bottom "1px solid #DEDEE7"
                                                 :margin-bottom 30}

                                       :header {:display         "flex"
                                                :flex-direction  "row"
                                                :justify-content "space-between"}

                                       :title {:flexGrow       1
                                               :text-align     "left"
                                               :font           "normal normal 900 30px/35px Roboto"
                                               :letter-spacing "3.9px"
                                               :color          "#3A3668"
                                               :text-transform "uppercase"}

                                       :heading {:text-align     "left"
                                                 :font           "normal normal medium 16px/19px Roboto"
                                                 :letter-spacing "0px"
                                                 :color          "#3A3668"
                                                 :font-weight    :bold}

                                       :box {:padding-right  5
                                             :text-align     "left"
                                             :font           "normal normal medium 16px/19px Roboto"
                                             :font-weight    500
                                             :letter-spacing "0px"
                                             :color          "#3A3668"}

                                       :button {:textTransform  "none"
                                                :font           "normal normal medium 16px/19px Roboto"
                                                :letter-spacing "0px"
                                                :background     "#3428CA 0% 0% no-repeat padding-box"
                                                :color          "#ECEFF8"
                                                :border-radius  "8px"}

                                       :primary {:display         "flex"
                                                 :flex-direction  "row"
                                                 :justify-content :space-between
                                                 :text-align      "left"
                                                 :font            "normal normal medium 14px/15px Roboto"
                                                 :font-weight     500
                                                 :letter-spacing  "0px"
                                                 :color           "#3A3668"}

                                       :secondary {:text-align     "left"
                                                   :font           "normal normal 900 10px/11px Roboto"
                                                   :letter-spacing "0px"
                                                   :color          "#757295"}

                                       :details {:display :inline}

                                       :search {:margin        10
                                                :border-radius "5px"}

                                       :search-icon {:color "#3A3668"}

                                       :scroll-list {:overflow   :auto
                                                     :max-height 300}

                                       :progress {:borderRadius 4
                                                  :height       8
                                                  :width        "100%"}

                                       :progress-typography {:margin-top     4
                                                             :font           "normal normal 900 10px/11px Roboto"
                                                             :letter-spacing "0px"
                                                             :color          "#757295"
                                                             :opacity        1}
                                       :accordion {:margin "0px"}})))

(def type->label {"CONTINUOUS_TREE"       "Continuous: MCC tree"
                  "DISCRETE_TREE"         "Discrete: MCC tree"
                  "BAYES_FACTOR_ANALYSIS" "Discrete: Bayes Factor Rates"})

(defn completed-menu-item [{:keys [id readable-name of-type status new?]} classes]
  (let [[anchorElement setAnchorElement] (react/useState nil)
        handle-close                     #(setAnchorElement nil)
        open?                            (not (nil? anchorElement))
        error?                           (= "ERROR" status)]
    [list-item {:button true
                :on-click
                #(dispatch-n [[:router/navigate :route/analysis-results nil {:id id :tab "results"}]
                              (when new?
                                [:graphql/query {:query
                                                 "mutation TouchAnalysisMutation($analysisId: ID!) {
                                                             touchAnalysis(id: $analysisId) {
                                                               id
                                                               isNew
                                                             }
                                                           }"
                                                 :variables {:analysisId id}}])])}
     [list-item-text {:primary   (reagent/as-element [:div {:class-name (:primary classes)}
                                                    [:span (or readable-name "Unknown")]
                                                    (when error?
                                                      [chip {:label   "Error"
                                                             :size    :small
                                                             :variant "outlined"
                                                             :color   "secondary"}])
                                                    (when new?
                                                      [chip {:label   "New"
                                                             :size    :small
                                                             :variant "outlined"
                                                             :color   "primary"}])
                                                    [:div
                                                     [icon-button {:aria-label    "analysis kebab menu"
                                                                   :aria-controls "menu-kebab"
                                                                   :aria-haspopup true
                                                                   :color         "inherit"
                                                                   :style         {:padding 0}
                                                                   :on-click      (fn [event]
                                                                                    (setAnchorElement (.-currentTarget event))
                                                                                    (.stopPropagation event))}
                                                      [:img {:src (:kebab-menu icons)}]]
                                                     [menu {:id               "menu-kebab"
                                                            :anchorEl         anchorElement
                                                            :anchorOrigin     {:vertical   "top"
                                                                               :horizontal "right"}
                                                            :transform-origin {:vertical   "top"
                                                                               :horizontal "right"}
                                                            :keep-mounted     true
                                                            :open             open?
                                                            :on-close         handle-close}
                                                      [menu-item {:on-click (fn []
                                                                              (prn "TODO"))} "Edit"]
                                                      [menu-item {:on-click (fn []
                                                                              (prn "TODO"))} "Load different file"]
                                                      [menu-item {:on-click (fn []
                                                                              (prn "TODO"))} "Copy settings"]
                                                      [menu-item {:on-click (fn []
                                                                              (prn "TODO"))} "Delete"]]]])
                      :secondary (reagent/as-element [typography {:class-name (:secondary classes)}
                                                      (type->label of-type)])}]]))

(defn completed [classes {:keys [default-expanded?]}]
  (let [search-term        (re-frame/subscribe [::subs/search])
        completed-analysis (re-frame/subscribe [::subs/completed-analysis-search])
        new-completed      (re-frame/subscribe [::subs/new-completed-analysis])]
    (fn []
      (let [items     @completed-analysis
            new-count (count @new-completed)]
        [accordion {:defaultExpanded default-expanded?}
         [accordion-summary {:expand-icon (reagent/as-element [:img {:src (:dropdown icons)}])}
          [:div {:class-name (:header classes)}
           [:img {:src (:completed icons)}]
           [typography {:class-name (:heading classes)} "Completed data analysis"]
           (when (> new-count 0)
             [chip {:label   (str new-count " New")
                    :size    :small
                    :variant "outlined"
                    :color   "primary"}])]]
         [divider {:variant "fullWidth"}]
         ;; TODO : style it
         [search-bar {:value       (or "" @search-term)
                      :on-change   #(>evt [:general/set-search %])
                      :placeholder "Search"
                      :class-name  (:search classes)
                      :searchIcon  (reagent/as-element [search {:class-name (:search-icon classes)}])}]
         [accordion-details {:class-name (:details classes)}
          [list {:class-name (:scroll-list classes)}
           (doall
             (map (fn [{:keys [id] :as item}]
                    ^{:key id} [completed-menu-item (-> item
                                                        ;; TODO : for dev
                                                        #_(assoc :new? true)
                                                        #_(assoc :status "ERROR")
                                                        )
                                classes])
                  items))]]]))))

(defn queued-menu-item [{:keys [readable-name of-type progress]}
                        classes]
  [list-item {:button true}
   [:div
    [list-item-text {:primary   (reagent/as-element [:div {:class-name (:primary classes)}
                                                     [:span (or readable-name "Unknown")]])
                     :secondary (reagent/as-element [typography {:class-name (:secondary classes)}
                                                     (type->label of-type)])}]
    [linear-progress {:value      (* 100 progress)
                      :variant    "determinate"
                      :class-name (:progress classes)}]
    [typography {:class-name (:progress-typography classes)}
     (str (format-percentage progress 1.0) " finished")]]])

(defn queue [classes {:keys [default-expanded?]}]
  (let [queued-analysis (re-frame/subscribe [::subs/queued-analysis])]
    (fn []
      (let [items
            #_           [{:readable-name "Relaxed_sDollo_AllSingleton_v2"
                           :progress      0.3
                           :id            "fff-ff-fff"
                           :of-type       "CONTINUOUS_TREE"}]
            @queued-analysis
            queued-count (count items)]
        [accordion {:defaultExpanded default-expanded?}
         [accordion-summary {:expand-icon (reagent/as-element [:img {:src (:dropdown icons)}])}
          [:div {:class-name (:header classes)}
           [:img {:src (:queue icons)}]
           [typography {:class-name (:heading classes)} "Queue"]
           (when (> queued-count 0)
             [chip {:label   (str queued-count " Ongoing")
                    :size    :small
                    :variant "outlined"}])]]
         [divider {:variant "fullWidth"}]
         [accordion-details {:class-name (:details classes)}
          [list {:class-name (:scroll-list classes)}
           (doall
             (map (fn [{:keys [id] :as item}]
                    ^{:key id} [queued-menu-item item classes])
                  items))]]]))))

(def custom-accordion ((styles/with-styles (fn [_] {:root {:margin "0px"}
                                                    :expanded {:margin "0px"}}))
                       accordion))

(defn run-new [classes {:keys [default-expanded?]}]
  (let [items [{:main-label "Discrete:"         :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "discrete-mcc-tree"}}
               {:main-label "Discrete:"         :sub-label "Bayes factor rates"
                :target     :route/new-analysis :query     {:tab "discrete-rates"}}
               {:main-label "Continuous:"       :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "continuous-mcc-tree"}}]]
    
    [accordion
     {:defaultExpanded default-expanded?
      ;;:class-name (:accordion classes)
      :style {:margin "0px"}
      } 
     [accordion-summary {:expand-icon (reagent/as-element [:img {:src (:dropdown icons)}])}
      [:img {:src (:run-new icons)}]
      [typography {:class-name (:heading classes)} "Run new analysis"]]
     [divider {:variant "fullWidth"}]
     [accordion-details {:class-name (:details classes)}
      [list
       (doall
        (map-indexed (fn [index {:keys [main-label sub-label target query]}]
                       [list-item {:key      index
                                   :button   true
                                   :on-click #(>evt [:router/navigate target nil query])}
                        [box {:class-name (:box classes)}
                         main-label]
                        [list-item-text
                         {:class-name               (:secondary classes)
                          :secondary                sub-label
                          :secondaryTypographyProps {:align "left"}}]])
                     items))]]]))

(defn main-menu [classes]
  [:div.app-sidebar.panel
   [run-new classes {:default-expanded? true}]
   [completed classes {:default-expanded? true}]
   [queue classes {:default-expanded? true}]
   [button {:variant   "contained"
            :color     "primary"
            :size      "large"
            :className (:button classes)
            :startIcon (reagent/as-element [:img {:src (:run-new icons)}])
            :on-click  #(>evt [:router/navigate :route/new-analysis nil {:tab "continuous-mcc-tree"}])}
    "Run new analysis"]])

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
   [:div.logo 
    [:div.logo-img
     [:div.hex.hex1] [:div.hex.hex2] [:div.hex.hex3] [:div.hex.hex4] ]
    [:span.text "spread"]]])

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
