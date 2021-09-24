(ns analysis-viewer.events.maps
  "Handle events registered in events.cljs under :map/*"
  (:require [ajax.core :as ajax]
            [analysis-viewer.db :as db]
            [analysis-viewer.map-emitter :as map-emitter]
            [analysis-viewer.subs :as subs]
            [clojure.string :as string]
            [shared.math-utils :as math-utils]
            [shared.output-data :as output-data]))

;; screen-coord: [x,y]      coordinates in screen pixels, 0 <= x <= map-width, 0 <= y <= map-height
;; proj-coord:   [x,y]      coordinates in map projection coords, 0 <= x <= 360, 0 <= y <= 180
;; map-coord:    [lat,lon]  coordinates in map lat,long coords, -180 <= lon <= 180, -90 <= lat <= 90

(def map-proj-width  360)
(def map-proj-height 180)

(def max-scale 22)
(def min-scale 0.8)

;; TODO: grab this from config
(def s3-bucket-url "http://127.0.0.1:9000/spread-dev-uploads/")

(defn initialize [_ [_ maps analysis-data-url]]
  (let [load-maps-events (->> maps
                              (mapv (fn [map-code]
                                      (let [world-map? (string/ends-with? map-code "WORLD")]
                                        [:map/load-map (cond-> {:map/url (str s3-bucket-url "maps/country-code-maps/" map-code ".json")}
                                                         world-map? (assoc :map/z-index 0))]))))
        load-data-event [:map/load-data analysis-data-url]]
    {:db (db/initial-db) 
     :fx (->> (conj load-maps-events load-data-event)
              (map (fn [ev] [:dispatch ev])))}))

(defn map-loaded [{:keys [db]} [_ map-data geo-json-map]]
  {:db (update db :maps/data conj (-> map-data
                                      (update :map/z-index #(or % 100))
                                      (assoc :map/geo-json geo-json-map)))})

(defn build-attributes [all-attributes]
  (->> all-attributes
       (map (fn [{:keys [scale id] :as attr}]              
              [id
               (case scale
                 "linear" {:id id
                           :attribute/type :linear
                           :range (:range attr)}
                 "ordinal" {:id id
                            :attribute/type :ordinal
                            :domain (map str (:domain attr))})]))
       (into {})))

(defn data-loaded [{:keys [db]} [_ data]]
  (println "Loaded analysis of type " (:analysisType data) "with keys:" (keys data))
  (let [analysis-type (keyword (:analysisType data))
        {:keys [startTime endTime]} (:timeline data)
        timeline-start (when startTime (.getTime (js/Date. startTime)))
        timeline-end   (when endTime (.getTime (js/Date. endTime)))
        analysis-data (case analysis-type
                        :ContinuousTree (map-emitter/continuous-tree-output->map-data data)
                        :DiscreteTree   (map-emitter/discrete-tree-output->map-data data)
                        :BayesFactor    (map-emitter/bayes-output->map-data data))
        {:keys [min-x min-y max-x max-y] :as data-box} (-> (output-data/data-bounding-box data)
                                                           (math-utils/map-box->proj-box))
        padding 2]    
    {:db (cond-> db
           true (assoc :analysis/data analysis-data)
           true (assoc :analysis/data-box data-box)
           true (assoc :analysis.data/type analysis-type)
           true (assoc :analysis/attributes (build-attributes (into (:pointAttributes data)
                                                                    (:lineAttributes data))))
           timeline-start (assoc :analysis/date-range [timeline-start timeline-end])
           timeline-start (assoc :animation/frame-timestamp timeline-start)
           timeline-start (assoc :animation/crop [timeline-start timeline-end]))
     :dispatch [:map/set-view-box {:x1 (- min-x padding) :y1 (- min-y padding)
                                   :x2 (+ max-x padding) :y2 (+ max-y padding)}]}))

(defn load-map [_ [_ map-data]]
  {:http-xhrio {:method          :get
                :uri             (:map/url map-data)
                :timeout         8000 
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      [:map/map-loaded map-data]
                :on-failure      [:log-error]}})

(defn load-data [_ [_ analysis-data-url]]
  {:http-xhrio {:method          :get
                :uri             analysis-data-url
                :timeout         8000 
                :response-format (ajax/json-response-format {:keywords? true}) 
                :on-success      [:map/data-loaded]
                :on-failure      [:log-error]}})

(defn calc-proj-scale [{:keys [map/state]}]
  ;; TODO: this is correct on wide windows, make it work
  ;; on windows that are higher than wider
  
  (let [{:keys [height]} state
        width (* 2 height)]
    (/ width map-proj-width)))

(defn set-view-box [db [_ {:keys [x1 y1 x2 y2]}]]
  (let [proj-scale (calc-proj-scale db)
        {:keys [translate scale]} (math-utils/calc-zoom-for-view-box x1 y1 x2 y2 proj-scale)]
    (-> db
        (update :map/state merge {:translate translate
                                  :scale     scale}))))

(defn zoom [{:keys [map/state] :as db} [_ x y new-scale]]
  (let [proj-scale (calc-proj-scale db)
        {:keys [translate scale]} state
        screen-coords [x y]
        [proj-x proj-y] (math-utils/screen-coord->proj-coord translate scale proj-scale screen-coords)]
    (update db :map/state
            (fn [{:keys [translate scale] :as map-state}]
              (let [scaled-proj-x (* proj-x scale proj-scale)
                    scaled-proj-y (* proj-y scale proj-scale)
                    new-scaled-proj-x (* proj-x new-scale proj-scale)
                    new-scaled-proj-y (* proj-y new-scale proj-scale)
                    x-scale-diff (- scaled-proj-x new-scaled-proj-x)
                    y-scale-diff (- scaled-proj-y new-scaled-proj-y)
                    [tx ty] translate
                    new-translate-x (+ tx x-scale-diff)
                    new-translate-y (+ ty y-scale-diff)]
                
                (if (< min-scale new-scale max-scale)
                  (assoc map-state
                         :translate [(int new-translate-x) (int new-translate-y)]
                         :scale new-scale)
                  map-state))))))

(defn zoom-inc [{:keys [map/state] :as db} [_ {:keys [delta x y]}]]
  (let [{:keys [scale]} state
        zoom-dir (if (pos? delta) -1 1)
        new-scale (+ scale (* zoom-dir 0.8))]
    (zoom db [nil x y new-scale])))

(defn map-grab [db [_ {:keys [x y]}]]
  (-> db
      (assoc-in [:map/state :grab :screen-current]  [x y])))

(defn map-release [db _]
  (update db :map/state dissoc :grab))

(defn drag [{:keys [map/state] :as db} [_ {:keys [x y]}]]
  (let [current-screen-coord [x y]]
    (if (:grab state)
      (let [[screen-x screen-y] current-screen-coord
            before-screen-coord (-> state :grab :screen-current)
            [before-screen-x before-screen-y] before-screen-coord
            screen-drag-x (- screen-x before-screen-x)
            screen-drag-y (- screen-y before-screen-y)]
        (-> db            
            (assoc-in  [:map/state :grab :screen-current] current-screen-coord)
            (update-in [:map/state :translate 0] + screen-drag-x)
            (update-in [:map/state :translate 1] + screen-drag-y)))
      db)))

(defn zoom-rectangle-grab [db [_ {:keys [x y]}]]
  (-> db
      (assoc-in [:map/state :zoom-rectangle] {:origin [x y] :current [x y]})))

(defn zoom-rectangle-update [{:keys [map/state] :as db} [_ {:keys [x y]}]]
  (if (:zoom-rectangle state)
    (assoc-in db [:map/state :zoom-rectangle :current] [x y])
    db))

(defn zoom-rectangle-release [{:keys [db]} _]
  (let [{:keys [map/state]} db
        proj-scale (calc-proj-scale db)
        {:keys [translate scale zoom-rectangle]} state
        {:keys [origin current]} zoom-rectangle
        [x1 y1] (math-utils/screen-coord->proj-coord translate scale proj-scale origin)              
        [x2 y2] (math-utils/screen-coord->proj-coord translate scale proj-scale current)]     
    {:db (update db :map/state dissoc :zoom-rectangle)
     :dispatch [:map/set-view-box {:x1 x1 :y1 y1
                                   :x2 x2 :y2 y2}]}))

(defn toggle-show-world [{:keys [db]} _]
  {:db (update-in db [:map/state :show-world?] not)})

(defn download-current-as-svg [{:keys [db]} [_ ]]  
  (let [ui-params (:ui/parameters db)
        map-params (subs/build-map-parameters (:ui/parameters db)
                                              (:ui.switch-buttons/states db))]
    {:spread/download-current-map-as-svg {:geo-json-map (subs/geo-json-data-map (:maps/data db))
                                          :analysis-data (vals (subs/colored-and-filtered-data (:analysis/data db)
                                                                                               (:ui/parameters db)
                                                                                               (:analysis.data/filters db)))
                                          :data-box (:analysis/data-box db)
                                          :time (let [[df dt] (:analysis/date-range db)]
                                                  (math-utils/calc-perc df dt (:animation/frame-timestamp db)))
                                          :params (merge ui-params
                                                         map-params
                                                         (:ui.switch-buttons/states db))}}))

(def tick-duration 50) ;; milliseconds

(defn advance-frame-timestamp [timestamp speed plus-or-minus]
  (let [day-in-millis (* 24 60 60 1000)
        delta (/ (* speed day-in-millis) (/ 1000 tick-duration))]
    (plus-or-minus timestamp delta)))

(defn ticker-tick [{:keys [db]} _]
  (let [{:keys [animation/frame-timestamp animation/crop animation/speed]} db
        [_ crop-high] crop
        next-ts (advance-frame-timestamp frame-timestamp speed +)]
    
    (if (>= next-ts crop-high)
      {:db (assoc db :animation/frame-timestamp crop-high)
       :ticker/stop nil}
      
      {:db (assoc db :animation/frame-timestamp next-ts)})))

(defn animation-prev [{:keys [animation/frame-timestamp animation/crop animation/speed] :as db} _]  
  (let [[crop-low _] crop
        next-ts (advance-frame-timestamp frame-timestamp speed -)]
    (if (<= next-ts crop-low)
      (assoc db :animation/frame-timestamp crop-low)
      (assoc db :animation/frame-timestamp next-ts))))

(defn animation-next [{:keys [animation/frame-timestamp animation/crop animation/speed] :as db} _]  
  (let [[_ crop-high] crop
        next-ts (advance-frame-timestamp frame-timestamp speed +)]
    (if (>= next-ts crop-high)
      (assoc db :animation/frame-timestamp crop-high)
      (assoc db :animation/frame-timestamp next-ts))))

(defn animation-reset [{:keys [animation/crop] :as db} [_ dir]]  
  (let [[crop-low crop-high] crop
        ts (case dir
             :start crop-low
             :end   crop-high)]
    (assoc db :animation/frame-timestamp ts)))

(defn animation-toggle-play-stop [{:keys [db]} _]
  (if (= (:animation/state db) :play)
    {:db (assoc db :animation/state :stop)
     :ticker/stop nil}
    {:db (assoc db :animation/state :play)
     :ticker/start {:millis tick-duration}}))

(defn animation-set-crop [db [_ [crop-low-millis crop-high-millis]]]  
  (-> db
      (assoc :animation/crop [crop-low-millis crop-high-millis])
      (assoc :animation/frame-timestamp crop-low-millis)))

(defn animation-set-speed [db [_ new-speed]]
  (assoc db :animation/speed new-speed))

(defn set-dimensions [db [_ {:keys [width height]}]]
  (println "Reseting map dimensions to " width "x" height)
  (-> db
      (assoc-in [:map/state :width] width)
      (assoc-in [:map/state :height] height)))
 

(defn show-object-attributes [db [_ object-id coord]]
  (assoc db
         :analysis/selected-object-id object-id
         :map/popup-coord coord))

(defn hide-object-attributes [db _]
  (dissoc db :analysis/selected-object-id :map/popup-coord))

(defn show-object-selector [db [_ objects-ids coord]]
  (assoc db
         :analysis/possible-objects-ids (into [] objects-ids)
         :map/popup-coord coord))

(defn hide-object-selector [db _]  
  (dissoc db
          :analysis/possible-objects-ids
          :map/popup-coord
          :analysis/highlighted-object-id))

(defn highlight-object [db [_ object-id]]
  (assoc db :analysis/highlighted-object-id object-id))
