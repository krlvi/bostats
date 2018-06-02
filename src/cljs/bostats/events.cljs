(ns bostats.events
  (:require [re-frame.core :as re-frame]
            [bostats.db :as db]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [clojure.string :as str]))

(re-frame/reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-db
  :selected-areas (fn [db [_ area]] (assoc-in db [:selected-areas area] (not (area (:selected-areas db))))))

(re-frame/reg-event-db
  :min-room (fn [db [_ x]] (assoc db :min-room (if (= x "min") nil x))))

(re-frame/reg-event-db
  :max-room (fn [db [_ x]] (assoc db :max-room (if (= x "max") nil x))))

(re-frame/reg-event-db
  :min-boarea (fn [db [_ x]] (assoc db :min-boarea (if (= x "min") nil x))))

(re-frame/reg-event-db
  :max-boarea (fn [db [_ x]] (assoc db :max-boarea (if (= x "max") nil x))))

(re-frame/reg-event-db
  :sold-age (fn [db [_ x]] (assoc db :sold-age (if (= x "∞ månad") nil (first (str/split x " "))))))

(re-frame/reg-event-db
  :resolution (fn [db [_ x]] (assoc db :resolution x)))

(defn format-stats
  [rsp]
  (->>
    rsp
    :stats
    (map #(vec [(let [v (vals (:date %))] (+ (* 100 (first v)) (second v))) (:price-sqm-stats %)]))
    (reduce conj {})
    sort
    (into (sorted-map))))

(re-frame/reg-event-db
  :good-post-result
  (fn [db [_ x]]
    (->
      db
      (assoc :stats (format-stats x))
      (assoc :loading false))))

(re-frame/reg-event-db
  :bad-post-result  (fn [db [_ _]] (assoc db :loading false)))

(defn url-to-scrape
  [db]
  (let [base-url "https://www.hemnet.se/salda/bostader?"
        location-ids {:vasastan    "location_ids%5B%5D=898754"
                      :ostermalm   "location_ids%5B%5D=473448"
                      :kungsholmen "location_ids%5B%5D=473379"
                      :sodermalm   "location_ids%5B%5D=898472"
                      :danderyd    "location_ids%5B%5D=473325"
                      :bergshamra  "location_ids%5B%5D=473502"
                      :norra-d     "location_ids%5B%5D=909910"}]
    (str base-url (str/join "&" (vals (select-keys location-ids (keys (into {} (filter val (:selected-areas db)))))))
         (str "&sold_age=" (if (:sold-age db) (str (:sold-age db) "m") "all")
              (if (:min-room db) (str "&rooms_min=" (:min-room db)))
              (if (:max-room db) (str "&rooms_max=" (:max-room db)))
              (if (:min-boarea db) (str "&living_area_min=" (:min-boarea db)))
              (if (:max-boarea db) (str "&living_area_max=" (:max-boarea db)))
              "&page="))))

(re-frame/reg-event-fx
  :fetch-stats
  (fn [{:keys [db]} _]
    {:db         (assoc db :loading true)
     :http-xhrio {:method          :post
                  :uri             "https://5xy70r5hp8.execute-api.eu-west-1.amazonaws.com/forfunandprofit/sale-stats"
                  :params          {:url (url-to-scrape db) :resolution (:resolution db)}
                  :timeout         29000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:good-post-result]
                  :on-failure      [:bad-post-result]}}))

(re-frame/reg-event-db
  :chart
  (fn [db [_ x]]
    (assoc db :chart x)))
