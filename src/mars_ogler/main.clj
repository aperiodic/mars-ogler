(ns mars-ogler.main
  (:use [mars-ogler.routes :only [ogler-handler]]
        [mars-ogler.scrape :only [scrape-loop! setup-state!]]
        [ring.adapter.jetty :only [run-jetty]])
  (:gen-class))

(defn -main
  [& _]
  (setup-state!)
  (run-jetty ogler-handler {:join? false, :port 2001})
  (scrape-loop!))
