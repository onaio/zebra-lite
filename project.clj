(defn js-dir
      "Prefix with full JavaScript directory."
      [path]
      (str "resources/public/js/" path))

(defproject onaio/zebra-lite "0.1.0-SNAPSHOT"
  :description "A lite Zebra Version"
  :url "https://github.com/onaio/zebra-lite"
  :license {:name "Apache 2 License"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [;; ZEBRA LITE REQUIREMENTS
                 [onaio/milia "0.1.0-SNAPSHOT"]
                 [org.clojars.onaio/hatti "0.1.0-SNAPSHOT"]
                 [com.google.guava/guava "16.0"]
                 [compojure "1.3.2"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3196"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.logging "0.2.6"]
                 [ring.middleware.logger "0.5.0"]
                 [ring/ring-defaults "0.1.4"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 ;; ring middleware
                 [ring/ring-json "0.3.1"]
                 [hiccup "1.0.5"]
                 [rm-hull/ring-gzip-middleware "0.1.7"]
                 [slingshot "0.12.2"]
                 [clj-time "0.7.0"]
                 [inflections "0.9.7"]
                 [clavatar "0.2.1"]
                 [com.keminglabs/cljx "0.6.0" :exclusions [org.clojure/clojure]]
                 [clj-redis-session "2.1.0"]]

  :plugins [[com.keminglabs/cljx "0.6.0" :exclusions [org.clojure/clojure]]
            [lein-cljsbuild "1.0.5"]
            [lein-midje "3.1.3"]
            [lein-environ "1.0.0"]
            [lein-pdo "0.1.1"]
            [lein-kibit "0.0.8"]
            [lein-ring "0.7.1"]
            [lein-cloverage "1.0.2"]]
  :jvm-opts ^:replace ["-Xmx1g"]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :env {:debug-api? false
                         :debug false
                         :jetty-min-threads 10
                         :jetty-max-threads 80
                         :std-out "/dev/stdout"
                         :file "/var/log/ona-viewer/current"}}
             :uberjar {:env {:debug-api? false
                             :jetty-min-threads 10
                             :jetty-max-threads 80}
                       :aot :all}}

  :prep-tasks [["cljx" "once"] "javac" "compile"]
  :source-paths ["src/clj"
                 "target/generated/src/clj"]
  :test-paths ["tests/clj" "target/generated/tests/clj"]
  :cljsbuild {
    :builds {:dev
              {:source-paths ["src/cljs"
                              "target/generated/src/cljs"]
               :compiler {:output-to ~(js-dir "lib/main.js")
                          :output-dir ~(js-dir "lib/out")
                          :optimizations :whitespace
                          :pretty-print true
                          :source-map ~(js-dir "lib/main.js.map")}}
              :test
              {:source-paths ["src/cljs"
                              "tests/cljs"
                              "target/generated/src/cljs"]
               :notify-command ["phantomjs"
                                "phantom/unit-test.js"
                                "phantom/unit-test.html"
                                "target/main-test.js"]
               :compiler {:output-to "target/main-test.js"
                          :optimizations :whitespace
                          :pretty-print true}}
              :prod
              {:source-paths ["src/cljs"
                              "target/generated/src/cljs"]
               :compiler {:output-to ~(js-dir "lib/main.js")
                          :output-dir ~(js-dir "lib/out-prod")
                          :optimizations :advanced
                          :pretty-print false}
               :jar true}}
    :test-commands {"unit-test"
                    ["phantomjs" "phantom/unit-test.js" "phantom/unit-test.html" "target/main-test.js"]}}
  :aliases {"up" ["pdo" "cljsbuild" "auto" "dev," "ring" "server-headless"]}
  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated/src/clj"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/src/cljs"
                   :rules :cljs}]}
  :ring {:handler ona.viewer.main/app}
  :main ^:skip-aot ona.viewer.main)
