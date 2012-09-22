(defproject mars-ogler "0.0.1-SNAPSHOT"
  :description "Holy cow, it's Mars!"
  :url "http://github.com/aperiodic/mars-ogler"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-time "0.4.4"]
                 [compojure "1.1.3"]
                 [enlive "1.0.1"]
                 [hiccup "1.0.0"]
                 [ring/ring-core "1.1.5"]
                 [ring/ring-jetty-adapter "1.1.5"]]
  :plugins [[lein-ring "0.7.1"]]
  :main mars-ogler.main
  :ring {:handler mars-ogler.routes/handler})
