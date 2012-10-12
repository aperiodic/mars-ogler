(ns mars-ogler.main
  (:use [clojure.tools.cli :only [cli]]
        [mars-ogler.routes :only [ogler-handler]]
        [mars-ogler.scrape :only [scrape-loop! setup-state!]]
        [ring.adapter.jetty :only [run-jetty]])
  (:gen-class))

(defn -main
  [& args]
  (let [[opts _ bnnr] (cli args
                           ["-h" "--help" "Display this help banner and exit"
                            :flag true, :default false]
                           ["-p" "--port" :default 2001 :parse-fn #(Integer. %)]
                           ["-s" "--[no-]scrape"
                            "Check for new images once every minute"
                            :default true])
        {:keys [port]} opts]
    (when (:help opts)
      (println bnnr)
      (System/exit 0))
    (setup-state!)
    (if-not (:scrape opts)
      (run-jetty ogler-handler {:port port})
      (do
        (run-jetty ogler-handler {:join? false, :port port})
        (scrape-loop!)))))
