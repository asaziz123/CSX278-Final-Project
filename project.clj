(defproject engn-web "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring-server "0.4.0"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.2.1"]
                 [ring "1.6.1"]
                 [ring/ring-defaults "0.3.0"]
                 [ring/ring-json "0.4.0"]
                 [compojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [yogthos/config "0.8"]
                 [org.clojure/clojurescript "1.9.671"
                  :scope "provided"]
                 [secretary "1.2.3"]
                 [cljs-ajax "0.6.0"]
                 [com.andrewmcveigh/cljs-time "0.5.0"]
                 [reagent-material-ui "0.2.4"]
                 [cs4278.grading "0.1.2-SNAPSHOT"]
                 [venantius/accountant "0.2.0"
                  :exclusions [org.clojure/tools.reader]]
                 [auth0-ring "0.1.0"]]

  :plugins [[lein-environ "1.0.2"]
            [lein-cljsbuild "1.1.5"]
            [lein-exec "0.3.6"]
            ;[lein-midje "3.0.0"]
            ;[venantius/ultra "0.5.1"]
            ;[lein-codox "0.10.3"]
            [s3-wagon-private "1.3.0"]
            [com.jakemccrary/lein-test-refresh "0.18.1"]
            [lein-asset-minifier "0.2.7"
             :exclusions [org.clojure/clojure]]]

  :repositories [["private" {:url "s3p://cs4278/releases/" :creds :gpg}]
                 ["central" "http://repo1.maven.org/maven2"]
                 ["clojure" "http://build.clojure.org/releases"]
                 ["clojure-snapshots" "http://build.clojure.org/snapshots"]
                 ["clojars" "http://clojars.org/repo/"]]

   :aliases {"grade" ["exec" "-ep" "(use 'cs4278.grading) (grade sample-testsuite)"]}

  :ring {:handler engn-web.handler/app
         :uberwar-name "engn-web.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "engn-web.jar"

  :main engn-web.server

  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :test-paths ["test/clj"]

  :cljsbuild
  {:builds {:min
            {:source-paths ["src/cljs" "src/cljc" "env/prod/cljs"]
             :compiler
             {:output-to "target/cljsbuild/public/js/app.js"
              :output-dir "target/uberjar"
              :optimizations :advanced
              :pretty-print  false}}
            :app
            {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
             :figwheel {:on-jsload "engn-web.core/mount-root"}
             :compiler
             {:main "engn-web.dev"
              :asset-path "/js/out"
              :output-to "target/cljsbuild/public/js/app.js"
              :output-dir "target/cljsbuild/public/js/out"
              :source-map true
              :optimizations :none
              :pretty-print  true}}
            :test
            {:source-paths ["src/cljs" "src/cljc" "test/cljs"]
             :compiler {:main engn-web.doo-runner
                        :asset-path "/js/out"
                        :output-to "target/test.js"
                        :output-dir "target/cljstest/public/js/out"
                        :optimizations :whitespace
                        :pretty-print true}}

            :devcards
            {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
             :figwheel {:devcards true}
             :compiler {:main "engn-web.cards"
                        :asset-path "js/devcards_out"
                        :output-to "target/cljsbuild/public/js/app_devcards.js"
                        :output-dir "target/cljsbuild/public/js/devcards_out"
                        :source-map-timestamp true
                        :optimizations :none
                        :pretty-print true}}}}




  :figwheel
  {:http-server-root "public"
   :server-port 3450
   :nrepl-port 7002
   :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]

   :css-dirs ["resources/public/css"]
   :ring-handler engn-web.handler/app}

  :test-refresh {:refresh-dirs ["src/clj" "test/clj"]}


  :profiles {:dev {:repl-options {:init-ns engn-web.repl
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :dependencies [[binaryage/devtools "0.9.4"]
                                  [ring/ring-mock "0.3.1"]
                                  [ring/ring-devel "1.6.1"]
                                  [prone "1.1.4"]
                                  [figwheel-sidecar "0.5.11"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [devcards "0.2.3" :exclusions [cljsjs/react]]
                                  [pjstadig/humane-test-output "0.8.2"]]


                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.5.11"]
                             [lein-doo "0.1.6"]]


                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :env {:dev true}}

             :uberjar {:hooks [minify-assets.plugin/hooks]
                       :source-paths ["env/prod/clj"]
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
                       :env {:production true}
                       :aot :all
                       :omit-source true}})
