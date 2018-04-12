(defproject bostats "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.908"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.5"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [cljsjs/chartjs "2.7.0-0"]
                 [re-frame-datatable "0.6.0"]
                 [clj-http "3.7.0"]
                 [hickory "0.7.1"]
                 [clj-time "0.14.2"]
                 [com.stuartsierra/frequencies "0.1.0"]
                 [uswitch/lambada "0.1.2"]
                 [org.clojure/core.async "0.4.474"]
                 [cheshire "5.8.0"]]

  :main ^:skip-aot bostats.core

  :target-path "target/%s"

  :plugins [[lein-cljsbuild "1.1.5"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.4"]
                   [day8.re-frame/re-frame-10x "0.3.0"]
                   [day8.re-frame/tracing "0.5.0"]]

    :plugins      [[lein-figwheel "0.5.11"]]}
   :prod { :dependencies [[day8.re-frame/tracing-stubs "0.5.0"]]}
   :uberjar {:aot :all}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "bostats.core/mount-root"}
     :compiler     {:main                 bostats.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload
                                           day8.re-frame-10x.preload]
                    :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true
                                           "day8.re_frame.tracing.trace_enabled_QMARK_" true}
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            bostats.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}


    ]}

  )
