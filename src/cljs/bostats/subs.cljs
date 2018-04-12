(ns bostats.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(defn val-with-default
  [key default]
  (fn [db _] (let [v (key db)] (if v v default))))

(re-frame/reg-sub :min-room (val-with-default :min-room "min"))

(re-frame/reg-sub :max-room (val-with-default :max-room "max"))

(re-frame/reg-sub :min-boarea (val-with-default :min-boarea "min"))

(re-frame/reg-sub :max-boarea (val-with-default :max-boarea "max"))

(re-frame/reg-sub :sold-age (fn [db _] (let [v (:sold-age db)] (if-not v "∞ månad" (str v " månad")))))

(re-frame/reg-sub :resolution (fn [db _] (:resolution db)))

(re-frame/reg-sub :debug (fn [db _] db))

(re-frame/reg-sub :loading (fn [db _] (:loading db)))

(re-frame/reg-sub :selected-areas (fn [db _] (:selected-areas db)))

(re-frame/reg-sub :stats (fn [db _] (:stats db)))

(re-frame/reg-sub :chart (fn [db _] (:chart db)))

(re-frame/reg-sub
  :table-data
  (fn [db _]
    (let [stats (:stats db)
          table-data (if stats (mapv (fn [[k v]] (assoc v :date k)) stats) [])]
      table-data))
  )