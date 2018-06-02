(ns bostats.views
  (:require [re-frame.core :as re-frame]
            [bostats.subs :as subs]
            [cljsjs.chartjs]
            [re-frame-datatable.core :as dt]
            [re-frame-datatable.views :as dtv]
            [reagent.core :as reagent]
            ))

(def <sub (comp deref re-frame/subscribe))

(def >evt re-frame/dispatch)

(defn checkbox
  [key name]
  [:div.column [:label [:input {:type :checkbox :checked (key (<sub [:selected-areas])) :on-change #(>evt [:selected-areas key])}] name]])

(defn select
  [sub-key evt-key range default-select & [option-suffix]]
  [:select.form-control {:id :many-options :value (<sub [sub-key]) :on-change #(>evt [evt-key (-> % (.-target) (.-value))])}
   [:option default-select]
   (for [i range]
     ^{:key i} [:option (str i option-suffix)])])

(defn config-view
  []
  [:div
   [:div
    [:div.row
     (checkbox :vasastan "Vasastan")
     (checkbox :ostermalm "Östermalm")
     (checkbox :kungsholmen "Kungsholmen")
     (checkbox :sodermalm "Södermalm")
     (checkbox :danderyd "Danderyd")
     (checkbox :bergshamra "Bergshamra")
     (checkbox :norra-d "Norra Djurgårdsstaden")]
    [:div.row
     [:div.column
      "Antal rum"
      [:div.row
       [:div.column (select :min-room :min-room (range 1 16) "min")]
       [:div.column (select :max-room :max-room (range 1 16) "max")]]]
     [:div.column
      "Boarea m²"
      [:div.row
       [:div.column (select :min-boarea :min-boarea (range 20 255 5) "min")]
       [:div.column (select :max-boarea :max-boarea (range 20 255 5) "max")]]]
     [:div.column
      "Såld inom de senaste"
      (select :sold-age :sold-age (range 1 25) "∞ månad" " månad")]
     [:div.column
      "Resolution"
      [:select.form-control {:id :many-options :value (<sub [:resolution]) :on-change #(>evt [:resolution (-> % (.-target) (.-value))])}
       [:option {:key :monthly} "month"]
       [:option {:key :weekly} "week"]]]
     [:div.column
      [:br]
      [:input {:type     "button" :value "Fetch stats!"
               :disabled (<sub [:loading])
               :on-click #(>evt [:fetch-stats])}]]]]])

(defn axis
  [id label position max]
  {:id id :type "linear" :position position :ticks {:min 0 :max max} :scaleLabel {:display true :labelString label}})

(defn construct-chart []
  (let [context (.getContext (.getElementById js/document "stats-chart") "2d")
        y-axes [(axis "A" "Price per m²" "left" 140000) (axis "B" "Apartments sold" "right" 2000)]
        x-axes [{:barThickness 15}]
        chart-config {:type "bar" :options {:scales {:yAxes y-axes :xAxes x-axes}}}
        chart (js/Chart. context (clj->js chart-config))]
    (>evt [:chart chart])))

(defn stats->datasets
  [stats]
  [{:label "Volume" :data (map :sample-count (vals stats)) :type "bar" :yAxisID "B" :backgroundColor "rgba(211, 84, 0,0.4)"}
   {:label "Mean" :data (map :mean (vals stats)) :type "line" :backgroundColor "rgba(127, 140, 141,0.2)"}
   {:label "Median" :data (map :median (vals stats)) :type "line" :backgroundColor "rgba(46, 204, 113,0.2)"}
   {:label "75th percentile" :data (map #(get-in % [:percentiles :75]) (vals stats)) :type "line" :backgroundColor "rgba(52, 152, 219,0.2)"}
   {:label "90th percentile" :data (map #(get-in % [:percentiles :90]) (vals stats)) :type "line" :backgroundColor "rgba(155, 89, 182,0.2)"}])

(defn update-chart
  [chart stats]
  (let [chart-data {:labels   (keys stats)
                    :datasets (stats->datasets stats)}]
    (set! (-> chart (.-data)) (clj->js chart-data))
    (.update chart)))

(defn stats-chart
  []
  (reagent/create-class
    {:component-did-mount          #(construct-chart)
     :component-will-receive-props #(update-chart (<sub [:chart]) (<sub [:stats]))
     :display-name                 "stats-chart"
     :reagent-render               (fn [] [:canvas {:id "stats-chart" :width "500" :height "280"}])}))

(defn table-num [x]
  [:span (int x)])

(defn dt-column
  [label key]
  {::dt/column-key key, ::dt/column-label label ::dt/sorting {::dt/enabled? true} ::dt/render-fn table-num})

(defn stats-raw-data-table []
  [:div
   [dt/datatable
    :stats-raw-data
    [:table-data]
    [(dt-column "Date" [:date])
     (dt-column "Samples" [:sample-count])
     (dt-column "Mean" [:mean])
     (dt-column "Stdev" [:stdev])
     (dt-column "25th" [:percentiles :25])
     (dt-column "50th" [:percentiles :50])
     (dt-column "75th" [:percentiles :75])
     (dt-column "90th" [:percentiles :90])
     (dt-column "95th" [:percentiles :95])
     (dt-column "99th" [:percentiles :99])
     (dt-column "99.9th" [:percentiles :99.9])]
    {::dt/pagination {::dt/enabled? true, ::dt/per-page 8}}]
   [dtv/default-pagination-controls :stats-raw-data [:table-data]]])

(defn single-page-app []
  (fn []
    [:div
     [:h1 "Clojure for fun and for profit"]
     [config-view]
     [:br]
     [stats-chart (<sub [:stats])]
     [:br]
     [stats-raw-data-table]]))
