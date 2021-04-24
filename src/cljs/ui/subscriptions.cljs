(ns ui.subscriptions
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::config
  (fn [db _]
    (get db :config)))

(re-frame/reg-sub
  ::users
  (fn [db _]
    (-> db :users
        (dissoc :authorized-user))))

(re-frame/reg-sub
  ::authorized-user
  (fn [db _]
    (-> db :users :authorized-user)))

(re-frame/reg-sub
  ::discrete-tree-parsers
  (fn [db _]
    (get db :discrete-tree-parsers)))

(re-frame/reg-sub
  ::discrete-tree-parser
  :<- [::discrete-tree-parsers]
  (fn [discrete-tree-parsers [_ id]]
    (get discrete-tree-parsers id)))

(re-frame/reg-sub
 ::continuous-tree-parsers
 (fn [db _]
   (get db :continuous-tree-parsers)))

(re-frame/reg-sub
 ::continuous-tree-parser
 :<- [::continuous-tree-parsers]
 (fn [continuous-tree-parsers [_ id]]
   (get continuous-tree-parsers id)))

;; TODO : queued parsers

;; TODO : completed parsers

(re-frame/reg-sub
 ::active-continuous-tree-parser
 (fn [db _]
   (let [id (get-in db [:new-analysis :continuous-mcc-tree :continuous-tree-parser-id])]
     (get (get db :continuous-tree-parsers) id))))

(re-frame/reg-sub
  ::continuous-mcc-tree
  (fn [db]
    (get-in db [:new-analysis :continuous-mcc-tree])))

(re-frame/reg-sub
  ::continuous-mcc-tree-field-errors
  (fn [db]
    (get-in db [:new-analysis :continuous-mcc-tree :errors])))

(re-frame/reg-sub
 ::active-discrete-tree-parser
 (fn [db _]
   (let [id (get-in db [:new-analysis :discrete-mcc-tree :discrete-tree-parser-id])]
     (get (get db :discrete-tree-parsers) id))))

(re-frame/reg-sub
  ::discrete-mcc-tree
  (fn [db]
    (get-in db [:new-analysis :discrete-mcc-tree])))

(re-frame/reg-sub
  ::discrete-mcc-tree-field-errors
  (fn [db]
    (get-in db [:new-analysis :discrete-mcc-tree :errors])))




(comment
  @(re-frame/subscribe [::authorized-user])
  @(re-frame/subscribe [::discrete-tree-parsers])
  @(re-frame/subscribe [::discrete-tree-parser "60b08880-03e6-4a3f-a170-29f3c75cb43f"]))
