(ns shared.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :collapsible-tabs/tabs
 (fn [db _]
   (:ui.collapsible-tabs/tabs db)))

(reg-sub
 :collapsible-tabs/open?
 :<- [:collapsible-tabs/tabs]
 (fn [tabs [_ tab-id]]
   (get-in tabs [tab-id])))
