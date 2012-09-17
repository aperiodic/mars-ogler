(defproject mars-ogler "0.0.1-SNAPSHOT"
  :description "Holy cow, it's Mars!"
  :url "http://github.com/aperiodic/mars-ogler"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-time "0.4.4"]
                 [compojure "1.1.1"]
                 [hiccup "1.0.0"]
                 [enlive "1.0.1"]]
  :plugins [[lein-ring "0.7.1"]]
  :ring {:handler mars-ogler.routes/handler})
