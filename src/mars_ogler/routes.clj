(ns mars-ogler.routes
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clojure.string :as str]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [mars-ogler.views :as views])
  (:use [compojure.core :only [defroutes GET]]
        [ring.middleware cookies
                         keyword-params
                         params]))

(defn parse-cookie-params
  [{cams "cams", per-page "per-page", sorting "sorting", thumbs "thumbs"}]
  {:cams (str/split (:value cams) #" "), :per-page (:value per-page)
   :sorting (:value sorting), :thumbs (:value thumbs)})

(defn parse-params
  [{:keys [cams page per-page sorting thumbs query-string]
    :or {cams ["mahli" "mastcam" "navcam"]
         page "1"
         per-page "25"
         sorting "released"
         thumbs "no"}
    :as params}]
  (let [cams' (if (vector? cams)
                (set (map keyword cams))
                #{(keyword cams)})
        page' (Integer/parseInt page)
        per-page' (Integer/parseInt per-page)
        sorting' (keyword sorting)
        thumbs' (keyword thumbs)]
    (merge params
           {:cams cams', :page page', :per-page per-page', :sorting sorting'
            :thumbs thumbs', :query-string (or query-string "")})))

(defn visit-tick
  [visit-last visit-start visit-recent]
  (let [now (cvt-time/to-long (time/now))
        visit-last (if (nil? visit-last) now (Long/parseLong visit-last))
        visit-start (if (nil? visit-start) now (Long/parseLong visit-start))
        visit-recent (if (nil? visit-recent) now (Long/parseLong visit-recent))]
    (if (> (- now visit-recent) (* 10 60 1000))
      [visit-recent now now]
      [visit-last visit-start now])))

(defroutes ogler-routes
  (GET "/" [& params :as {:keys [query-string cookies]}]
    (let [[visit-last
           visit-start
           visit-recent] (visit-tick (get-in cookies ["visit-last" :value])
                                     (get-in cookies ["visit-start" :value])
                                     (get-in cookies ["visit-recent" :value]))
          parsed-params (-> cookies
                          parse-cookie-params
                          (merge params)
                          (assoc :query-string query-string
                                 :visit-last visit-last
                                 :visit-start visit-start
                                 :visit-recent visit-recent)
                          parse-params)]
      {:status 200
       :body (views/index parsed-params)
       :cookies (-> (select-keys params [:per-page :sorting :thumbs])
                  (assoc :visit-last visit-last
                         :visit-start visit-start
                         :visit-recent visit-recent
                         :cams (->> (:cams parsed-params)
                                 (map name)
                                 (str/join " "))))}))
  (route/resources "/")
  (route/not-found "You have wandered into a maze of twisty passages, all alike"))

(def ogler-handler
  (-> ogler-routes
    wrap-keyword-params
    wrap-params
    wrap-cookies))
