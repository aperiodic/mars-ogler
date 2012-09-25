(ns mars-ogler.routes
  (:require [clojure.string :as str]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [mars-ogler.views :as views])
  (:use [compojure.core :only [defroutes GET]]
        [ring.middleware cookies
                         keyword-params
                         params]))

(defn parse-cookies
  [{cams "cams", per-page "per-page", sorting "sorting", thumbs "thumbs"}]
  {:cams (str/split (:value cams) #" "), :per-page (:value per-page)
   :sorting (:value sorting), :thumbs (:value thumbs)})

(defn parse-params
  [{:keys [cams page per-page sorting thumbs query-string]
    :or {cams ["mahli" "mastcam" "navcam"]
         page "1"
         per-page "25"
         sorting "released"
         thumbs "no"}}]
  (let [cams' (if (vector? cams)
                (set (map keyword cams))
                #{(keyword cams)})
        page' (Integer/parseInt page)
        per-page' (Integer/parseInt per-page)
        sorting' (keyword sorting)
        thumbs' (keyword thumbs)]
    {:cams cams', :page page', :per-page per-page', :sorting sorting'
     :thumbs thumbs', :query-string (or query-string "")}))

(defroutes ogler-routes
  (GET "/" [& params :as {:keys [query-string cookies]}]
    (let [parsed-params (-> cookies
                          parse-cookies
                          (merge params)
                          (assoc :query-string query-string)
                          parse-params)]
      {:status 200
       :body (views/index parsed-params)
       :cookies (-> (select-keys params [:per-page :sorting :thumbs])
                  (assoc :cams (->> (:cams parsed-params)
                                 (map name)
                                 (str/join " "))))}))
  (route/resources "/")
  (route/not-found "You have wandered into a maze of twisty passages, all alike"))

(def ogler-handler
  (-> ogler-routes
    wrap-keyword-params
    wrap-params
    wrap-cookies))
