(ns ui.analysis-results.continuous-mcc-tree
  (:require [goog.string :as gstr]
            [shared.components :refer [labeled-field]]
            [ui.time :as time]))


(defn continuous-mcc-tree [{:keys [readable-name x-coordinate-attribute-name y-coordinate-attribute-name
                                   most-recent-sampling-date
                                   timescale-multiplier]}]
  (let [most-recent-sampling-date (when most-recent-sampling-date
                                    (time/format most-recent-sampling-date))]
    [:div.field-table
     [:div.field-line
      [:div.field-card {:style {:grid-area "file"}}
       [:h4 "Tree file"]
       [labeled-field {:label "Name" :text readable-name}]]]
     [:div.field-line
      [:div.field-card
       [:h4 "Time scale"]
       [labeled-field {:label "Multiplier" :text timescale-multiplier}]]
      [:div.field-card
       [:h4 "Most recent sampling date"]
       [labeled-field {:label "YYYY/MM/DD" :text most-recent-sampling-date}]]]
     [:div.field-line
      [:div.field-card
       [:h4 "Locations"]
       [labeled-field {:label "X attribute name" :text x-coordinate-attribute-name}]]
      [:div.field-card
       [:h4 (gstr/unescapeEntities "&nbsp;")]
       [labeled-field {:label "Y attribute name" :text y-coordinate-attribute-name}]]]]))
