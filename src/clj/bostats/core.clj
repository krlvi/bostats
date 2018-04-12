(ns bostats.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [com.stuartsierra.frequencies :as freq]
            [uswitch.lambada.core :refer [deflambdafn]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.core.async :as async]))

(def url "https://www.hemnet.se/salda/bostader?location_ids%5B%5D=898754&location_ids%5B%5D=473379&location_ids%5B%5D=898472&location_ids%5B%5D=473448&item_types%5B%5D=bostadsratt&page=")

(defn get-the-pages
  [url last-page]
  (let
    [c (async/chan last-page)]
    (client/with-async-connection-pool
      {:timeout 5 :threads 8 :insecure? false :default-per-route 20}
      (doseq [i (range 1 (inc last-page))]
        (async/go
          (let
            [rsp
             (some-> (client/get (str url i) {:cookie-policy :standard})
                     :body
                     hickory/parse
                     hickory/as-hickory)]
            (async/>! c rsp)))))
    [c, last-page]))

(defn get-from-chan
  [c, n]
  (remove nil? (for [i (range n)] (async/poll! c))))

(defn pages
  [url last-page]
  (let
    [c (get-the-pages url last-page)]
    (Thread/sleep 3000)
    (apply get-from-chan c)))

(defn page
  [url page_num]
  (some-> (client/get (str url page_num) {:cookie-policy :standard})
          :body
          hickory/parse
          hickory/as-hickory))

(defn parse-int [number-string]
  (try (Integer/parseInt number-string)
       (catch Exception e nil)))

(defn page-items
  [page]
  (s/select (s/class "results__sold-normal-item") page))

(defn parse-date
  [string-date]
  (some-> string-date
          (clojure.string/replace #"januari" "01")
          (clojure.string/replace #"februari" "02")
          (clojure.string/replace #"mars" "03")
          (clojure.string/replace #"april" "04")
          (clojure.string/replace #"maj" "05")
          (clojure.string/replace #"juni" "06")
          (clojure.string/replace #"juli" "07")
          (clojure.string/replace #"augusti" "08")
          (clojure.string/replace #"september" "09")
          (clojure.string/replace #"oktober" "10")
          (clojure.string/replace #"november" "11")
          (clojure.string/replace #"december" "12")
          (clojure.string/replace #"[^0-9]" "-")
          (some->> (f/parse (f/formatter "dd-MM-yyyy"))
                   ((fn [x] {:year  (t/year x)
                             :week (.get (.weekOfWeekyear x))
                             :month (t/month x)})))))

(defn item-sale-date
  [item]
  (some-> item
          (some->> (s/select (s/class "sold-property-listing__price"))
                   first
                   (s/select (s/class "sold-property-listing__sold-date"))
                   first
                   :content
                   first)
          (clojure.string/split #"Såld")
          last
          (clojure.string/trim)
          (parse-date)))

(defn item-sale-price-sqm
  [item]
  (some-> item
          (some->> (s/select (s/class "sold-property-listing__price"))
                   first
                   (s/select (s/class "sold-property-listing__price-per-m2"))
                   first
                   :content
                   first)
          (clojure.string/replace #"[^0-9]" "")
          parse-int))

(defn item-sale-price
  [item]
  (some-> item
          (some->> (s/select (s/class "sold-property-listing__price"))
                   first
                   (s/select (s/class "sold-property-listing__subheading"))
                   first
                   :content
                   first)
          (clojure.string/replace #"[^0-9]" "")
          parse-int))

(defn item-sqm
  [item]
  (some-> item
          (some->> (s/select (s/class "sold-property-listing__subheading"))
                   first
                   :content
                   first)
          (clojure.string/split #"\n")
          second
          (clojure.string/replace #"[^0-9]" "")
          parse-int))

(defn item-rooms
  [item]
  (some-> item
          (some->> (s/select (s/class "sold-property-listing__subheading"))
                   first
                   :content
                   first)
          (clojure.string/split #"m")
          second
          (clojure.string/replace #"[^0-9]" "")
          parse-int))

(defn item-address
  [item]
  (some->> (s/select (s/class "sold-property-listing__heading") item)
           first
           (s/select (s/class "item-result-meta-attribute-is-bold"))
           first
           :content
           first))

(defn parsed-page-items
  [page]
  (map #(assoc {} :address (item-address %)
                  :sqm (item-sqm %)
                  :rooms (item-rooms %)
                  :sale-date (item-sale-date %)
                  :sale-price (item-sale-price %)
                  :sale-price-sqm (item-sale-price-sqm %)) (page-items page)))

(defn last-page
  [url]
  (some-> url
          (page 1)
          :content
          (nth 4)
          :content
          (nth 4)
          :content
          (nth 3)
          :content
          first
          (some->> (re-find #"page_total\":[0-9]*"))
          (clojure.string/split #":")
          last
          parse-int))

(defn all-items
  [url]
  (apply concat (map #(parsed-page-items %) (pages url (last-page url)))))

(defn frequency-stats
  [resolution items]
  (map (fn [k] {:date            (select-keys (:sale-date (first k)) [:year resolution])
                :price-sqm-stats (freq/stats (frequencies (remove nil? (map #(:sale-price-sqm %) k))))})
       items))

(defn price-stats-per-month
  [items resolution]
  (->>
    items
    (map #(select-keys (:sale-date %) [:year resolution]))
    set
    (map (fn [x] (filter #(= (select-keys (:sale-date %) [:year resolution]) x) items)))
    (frequency-stats resolution)
    (sort-by #(get-in % [:date resolution]))))

(defn format-lambda-response
  [body]
  {"statusCode"      200
   "headers"         {"Access-Control-Allow-Origin" "*"
                      "Access-Control-Allow-Headers" "Content-Type"}
   "isBase64Encoded" false
   "body"            (json/write-str body)})

(defn handle-event
  [event]
  (println "Got the following event: " (pr-str event))
  (let
    [url (-> event (get "body") (json/read-str)(get "url"))
     resolution (-> event (get "body") (json/read-str)(get "resolution") keyword)]
    (println "url is " url)
    (let [stats (sort-by #(get-in % [:date :month]) (price-stats-per-month (all-items url) resolution))]
      (format-lambda-response {"stats" stats}))))

(deflambdafn bostad-stats.core.run
             [in out ctx]
             (let [event (json/read (io/reader in))
                   res (handle-event event)]
               (with-open [w (io/writer out)]
                 (json/write res w))))

(defn -main
  "Run this thing"
  [& args]
  (println (doall (sort-by #(get-in % [:date :month]) (price-stats-per-month (all-items url) :week)))))