(ns bostats.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [bostats.events :as events]
            [bostats.views :as views]
            [bostats.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/single-page-app]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
