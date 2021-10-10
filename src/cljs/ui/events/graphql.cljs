(ns ui.events.graphql
  (:require [ajax.core :as ajax]
            [camel-snake-kebab.core :as camel-snake]
            [camel-snake-kebab.extras :as camel-snake-extras]
            [clojure.core.match :refer [match]]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [ui.utils :refer [>evt dispatch-n dissoc-in]]
            [ui.time :as time]))

(defn gql-name->kw [gql-name]
  (when gql-name
    (let [k (name gql-name)]
      (if (string/starts-with? k "__")
        (keyword k)
        (let [k     (if (string/ends-with? k "_")
                      (str (.slice k 0 -1) "?")
                      k)
              parts (string/split k "_")
              parts (if (< 2 (count parts))
                      [(string/join "." (butlast parts)) (last parts)]
                      parts)]
          (apply keyword (map camel-snake/->kebab-case parts)))))))

(defn gql->clj [m]
  (->> m
       (js->clj)
       (camel-snake-extras/transform-keys gql-name->kw)))

(defn- with-safe-date
  "turns YYYY/mm/dd representation to a js/Date that can be used with the date component
   NOTE: we should revisit how we treat this argument to avoid going bakc and forth between representations"
  [{:keys [most-recent-sampling-date] :as analysis}]
  (let [js-date (when most-recent-sampling-date
                  (new js/Date most-recent-sampling-date))]
    (assoc analysis :most-recent-sampling-date js-date)))

(defmulti handler
  (fn [_ key value]
    (cond
      (:error value) :api/error
      (vector? key)  (first key)
      :else          key)))

(defn- update-db [cofx fx]
  (if-let [db (:db fx)]
    (assoc cofx :db db)
    cofx))

(defn- safe-merge [fx new-fx]
  (reduce (fn [merged-fx [k v]]
            (when (= :db k)
              (assoc merged-fx :db v)))
          fx
          new-fx))

(defn- do-reduce-handlers
  [{:keys [db] :as cofx} f coll]
  (reduce (fn [fxs element]
            (let [updated-cofx (update-db cofx fxs)]
              (if element
                (safe-merge fxs (f updated-cofx element))
                fxs)))
          {:db db}
          coll))

(defn reduce-handlers
  [cofx response]
  (do-reduce-handlers cofx
                      (fn [fxs [k v]]
                        (handler fxs k v))
                      response))

(defn response [cofx [_ {:keys [data errors]}]]
  (when errors
    (log/error "Error in graphql response" {:error errors}))
  (reduce-handlers cofx (gql->clj data)))

(defn query
  [{:keys [db localstorage]} [_ {:keys [query variables on-success]
                                 :or   {on-success [:graphql/response]}}]]
  (let [url          (get-in db [:config :graphql :url])
        access-token (:access-token localstorage)]
    {:http-xhrio {:method          :post
                  :uri             url
                  :headers         (merge {"Content-Type" "application/json"
                                           "Accept"       "application/json"}
                                          (when access-token
                                            {"Authorization" (str "Bearer " access-token)}))
                  :body            (js/JSON.stringify
                                     (clj->js {:query     query
                                               :variables variables}))
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      on-success
                  :on-failure      [:log-error]}}))

(defn ws-authorize [{:keys [localstorage]} [_ {:keys [on-timeout]}]]
  (let [access-token (:access-token localstorage)]
    {:dispatch [:websocket/request :default
                {:message
                 {:type    "connection_init"
                  :payload {"Authorization"
                            (str "Bearer " access-token)}}
                 :on-response [:graphql/ws-authorized]
                 :on-timeout  on-timeout
                 :timeout     3000}]}))

(defn ws-authorize-failed [_ [_ why?]]
  (log/warn "Failed to authorize websocket connection" {:error why?})
  {:dispatch [:router/navigate :route/splash]})

(defn subscription-response [cofx [_ response]]
  (reduce-handlers cofx (gql->clj (get-in response [:payload :data]))))

(defn subscription [_ [_ {:keys [id query variables]}]]
  {:dispatch [:websocket/subscribe :default (name id)
              {:message
               {:type    "start"
                :payload {:variables     variables
                          :extensions    {}
                          :operationName nil
                          :query         query}}
               :on-message [:graphql/subscription-response]}]})

(defn unsubscribe [_ [_ {:keys [id]}]]
  {:dispatch [:websocket/unsubscribe :default (name id)]})

(defmethod handler :default
  [cofx k values]
  ;; NOTE: this is the default handler that is intented for queries and mutations
  ;; that have nothing to do besides reducing over their response values
  (log/debug "default handler" {:k k})
  (reduce-handlers cofx values))

;; TODO
(defmethod handler :upload-continuous-tree
  [{:keys [db]} _ {:keys [id status]}]
  ;; start the status subscription for an ongoing analysis
#_  (>evt [:graphql/subscription {:id        id
                                :query     "subscription SubscriptionRoot($id: ID!) {
                                              parserStatus(id: $id) {
                                                id
                                                status
                                                progress
                                                ofType
                                              }
                                            }"
                                :variables {:id id}}])
#_  {:db (-> db
           (assoc-in [:new-analysis :continuous-mcc-tree :id] id)
           (assoc-in [:analysis id :status] status))})

;; TODO
(defmethod handler :update-continuous-tree
  [{:keys [db]} _ {:keys [id status]}]
  #_(when (= "ARGUMENTS_SET" status)
    (dispatch-n [[:graphql/query {:query     "mutation QueueJob($id: ID!) {
                                                startContinuousTreeParser(id: $id) {
                                                  id
                                                  status
                                                }
                                              }"
                                  :variables {:id id}}]]))

  #_{:db (assoc-in db [:analysis id :status] status)})

;; TODO
(defmethod handler :start-continuous-tree-parser
  [{:keys [db]} _ {:keys [id status]}]
#_  {:db (assoc-in db [:analysis id :status] status)})

;; TODO
(defmethod handler :get-continuous-tree
  [{:keys [db]} _ {:keys [id attribute-names] :as tree}]
#_  (let [active-analysis-id (-> db :new-analysis :continuous-mcc-tree :id)]
    {:db (cond-> db
           (= id active-analysis-id)
           (assoc-in [:new-analysis :continuous-mcc-tree :attribute-names] attribute-names)

           true
           (update-in [:analysis id] merge tree))}))

(defmethod handler :upload-discrete-tree
  [{:keys [db]} _ {:keys [id] :as analysis}]
  (>evt [:graphql/subscription {:id        id
                                :query     "subscription SubscriptionRoot($id: ID!) {
                                                parserStatus(id: $id) {
                                                  id
                                                  readableName
                                                  status
                                                  progress
                                                  ofType
                                                }
                                              }"
                                :variables {:id id}}])
  {:db (-> db
           ;; NOTE: id is the link between the ongoing analysis
           ;; and what we store under the `:analysis` key
           (assoc-in [:new-analysis :discrete-mcc-tree :id] id)
           (update-in [:analysis id] merge analysis))})

(defmethod handler :get-discrete-tree
  [{:keys [db]} _ {:keys [id most-recent-sampling-date] :as analysis}]
  ;; NOTE : parse date to an internal representation
  (let [most-recent-sampling-date (when most-recent-sampling-date
                                    (new js/Date most-recent-sampling-date))]
    {:db (-> db
             (update-in [:analysis id] merge analysis)
             (assoc-in [:analysis id :most-recent-sampling-date] most-recent-sampling-date))}))

(defmethod handler :update-discrete-tree
  [{:keys [db]} _ {:keys [id most-recent-sampling-date] :as analysis}]
  ;; NOTE : parse date to an internal representation
  (let [most-recent-sampling-date (when most-recent-sampling-date
                                    (new js/Date most-recent-sampling-date))]
    {:db (-> db
             (update-in [:analysis id] merge analysis)
             (assoc-in [:analysis id :most-recent-sampling-date] most-recent-sampling-date))}))

(defmethod handler :start-discrete-tree-parser
  [{:keys [db]} _ {:keys [id] :as analysis}]
  {:db (update-in db [:analysis id] merge (with-safe-date analysis))})

(defmethod handler :upload-bayes-factor-analysis
  [{:keys [db]} _ {:keys [id] :as analysis}]
  (>evt [:graphql/subscription {:id        id
                                :query     "subscription SubscriptionRoot($id: ID!) {
                                                           parserStatus(id: $id) {
                                                             id
                                                             readableName
                                                             status
                                                             progress
                                                             ofType
                                                           }}"
                                :variables {:id id}}])
  {:db (-> db
           ;; NOTE: id is the link between the ongoing analysis
           ;; and what we store under the `:analysis` key
           (assoc-in [:new-analysis :bayes-factor :id] id)
           (update-in [:analysis id] merge analysis))})

(defmethod handler :update-bayes-factor-analysis
  [{:keys [db]} _ {:keys [id] :as analysis}]
  {:db (update-in db [:analysis id] merge analysis)})

(defmethod handler :get-bayes-factor-analysis
  [{:keys [db]} _ {:keys [id] :as analysis}]
  {:db (update-in db [:analysis id] merge analysis)})

(defmethod handler :start-bayes-factor-parser
  [{:keys [db]} _ {:keys [id] :as analysis}]
  {:db (update-in db [:analysis id] merge analysis)})

(defmethod handler :parser-status
  [{:keys [db]} _ {:keys [id status of-type] :as parser}]
  (log/debug "parser-status handler" parser)
  (match [status of-type]
         ["ATTRIBUTES_PARSED" "CONTINUOUS_TREE"]
         ;; when worker has parsed attributes we can query them
         (let [ongoing-analysis-id (-> db :new-analysis :continuous-mcc-tree :id)]
           ;; NOTE : guard so that it does not continuosly query if the subscriptions is running
           (when-not (get-in db [:analysis ongoing-analysis-id :attribute-names])
             (>evt [:graphql/query {:query     "query GetContinuousTree($id: ID!) {
                                                  getContinuousTree(id: $id) {
                                                    id
                                                    attributeNames
                                                  }
                                                }"
                                    :variables {:id id}}])))

         ["ATTRIBUTES_PARSED" "DISCRETE_TREE"]
         ;; if worker parsed attributes query them
         ;; NOTE : guard so that it does not continuosly query if the subscriptions is running
         (let [ongoing-analysis-id (-> db :new-analysis :discrete-mcc-tree :id)]
           (when-not (get-in db [:analysis ongoing-analysis-id :attribute-names])
             (>evt [:graphql/query {:query     "query GetDiscreteTree($id: ID!) {
                                                  getDiscreteTree(id: $id) {
                                                    id
                                                    attributeNames
                                                  }
                                                }"
                                    :variables {:id id}}])))

         [(:or "SUCCEEDED" "ERROR") _]
         ;; if analysis ended stop the subscription
         (>evt [:graphql/unsubscribe {:id id}])

         :else nil)

  {:db (update-in db [:analysis id]
                    merge
                    ;; NOTE we can optimistically assume analysis is new
                    ;; since there is an ongoing subscription for it
                    (assoc parser :new? true))})

;; TODO
(defmethod handler :upload-time-slicer
  [{:keys [db]} _ {:keys [id status]}]
#_  {:db (-> db
           (assoc-in [:new-analysis :continuous-mcc-tree :time-slicer-parser-id] id)
           (assoc-in [:analysis id :status] status))})

(defmethod handler :get-user-analysis
  [{:keys [db]} _ analysis]
  (let [analysis (map #(rename-keys % {:is-new :new?}) analysis)]
    (>evt [:user-analysis-loaded])
    {:db (assoc db :analysis (zipmap (map :id analysis) analysis))}))

(defmethod handler :get-authorized-user
  [{:keys [db]} _ {:keys [id] :as user}]
  {:db (-> db
           (assoc-in [:users :authorized-user] user)
           (assoc-in [:users id] user))})

(defmethod handler :touch-analysis
  [{:keys [db]} _ {:keys [id is-new]}]
  {:db (assoc-in db [:analysis id :new?] is-new)})

(defmethod handler :delete-analysis
  [{:keys [db]} _ {:keys [id]}]
  {:db (dissoc-in db [:analysis id])})

(defmethod handler :delete-file
  [_ _ _]
  ;; nothing to do
  )

(defmethod handler :delete-user-data
  [{:keys [db]} _ _]
  (>evt [:router/navigate :route/home])
  {:db (-> db
           (dissoc :analysis)
           (dissoc :new-analysis))})

(defmethod handler :delete-user-account
  [{:keys [db]} _ {:keys [user-id]}]
  (>evt [:general/logout])
  (>evt [:router/navigate :route/splash])
  {:db (-> db
           (dissoc-in [:users :authorized-user])
           (dissoc-in [:users user-id]))})

(defmethod handler :google-login
  [_ _ {:keys [access-token]}]
  (re-frame/dispatch [:splash/login-success access-token]))

(defmethod handler :api/error
  [_ _ _]
  ;; NOTE: this handler is here only to catch errors
  )

(comment
  (>evt [:utils/app-db])
  (>evt [:graphql/query {:query     "query GetContinuousTree($id: ID!) {
                                                        getContinuousTree(id: $id) {
                                                          id
                                                          attributeNames
                                                        }
                                                      }"
                         :variables {:id "19512998-11cb-468a-9c13-f497a0920737"}}]))
