(defproject mars-ogler "1.0.1-SNAPSHOT"
  :description "Holy cow, it's Mars!"
  :url "http://github.com/aperiodic/mars-ogler"
  :license {:name "GNU Affero GPL"
            :url "http://www.gnu.org/licenses/agpl"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.cli "0.2.2"]
                 [cheshire "5.2.0"]
                 [clj-http "0.6.3"]
                 [clj-time "0.4.4"]
                 [compojure "1.1.3"]
                 [enlive "1.0.1"]
                 [hiccup "1.0.0"]
                 [ring/ring-core "1.1.6"]
                 [ring/ring-jetty-adapter "1.1.6"]]
  :plugins [[lein-ring "0.7.1"]]
  :main mars-ogler.main
  :uberjar-name "mars-ogler.jar"
  :jvm-opts ["-Xmx850m"
             "-XX:+UseConcMarkSweepGC"
             "-XX:+CMSIncrementalMode"
             "-XX:+UseCompressedOops"]
  :ring {:handler mars-ogler.routes/ogler-handler
         :init mars-ogler.images/setup!})
