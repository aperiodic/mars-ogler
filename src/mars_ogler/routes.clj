(ns mars-ogler.routes
  (:require [clj-http.client :as http]
            [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clojure.string :as str]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [mars-ogler.times :as times])
  (:use [compojure.core :only [defroutes GET]]
        [mars-ogler.views.index :only [index]]
        [mars-ogler.views.stereo :only [stereo]]
        [ring.middleware cookies
                         keyword-params
                         params]))

(def img-root "http://mars.jpl.nasa.gov")
(def expiration-date "Sun, 17 Jan 2038 19:14:07 GMT")

(defn parse-cookie-params
  [cookie-params]
  (let [keys [:cams :per-page :sorting :stereo :thumbs :view]
        kw->sk (zipmap keys (map name keys))]
    (into {} (->> (for [[kw sk] kw->sk]
                    (if-let [v (get-in cookie-params [sk :value])]
                      (if (= kw :cams)
                        [kw (str/split v #" ")]
                        [kw v])))
               (keep identity)))))

(defn parse-params
  [{:keys [cams page per-page sorting stereo thumbs query-string view]
    :or {cams ["mahli" "mastcam" "navcam"]
         page "1"
         per-page "25"
         sorting "released"
         stereo "yes"
         thumbs "no"
         view "list"}
    :as params}]
  (let [cams' (if (vector? cams)
                (set (map keyword cams))
                #{(keyword cams)})
        page' (Integer/parseInt page)
        sorting' (keyword sorting)
        stereo' (keyword stereo)
        thumbs' (keyword thumbs)
        view' (keyword view)
        per-page' (min (Integer/parseInt per-page)
                       (if (= view' :grid) 200 100))]
    (merge params
           {:cams cams', :page page', :per-page per-page', :sorting sorting'
            :stereo stereo', :thumbs thumbs', :query-string (or query-string "")
            :view view'})))

(defn visit-tick
  [visit-last visit-start visit-recent]
  (let [now (cvt-time/to-long (time/now))
        visit-last (if (nil? visit-last) now (Long/parseLong visit-last))
        visit-start (if (nil? visit-start) now (Long/parseLong visit-start))
        visit-recent (if (nil? visit-recent) now (Long/parseLong visit-recent))]
    (if (> (- now visit-recent) (* 10 60 1000))
      ;; curiositymsl.com updates every 15 minutes, and we scrape every minute
      [(- visit-recent (* 16 60 1000)) now now]
      [visit-last visit-start now])))

(defn set-expires
  [cookies]
  (into {} (for [[c v] cookies]
             [c {:value v, :expires expiration-date}])))

(defroutes ogler-routes
  (GET "/msl-raw-images/*" [* :as {{:strs [if-modified-since]} :headers}]
    (if if-modified-since ; note that we assume the images never change
      {:status 304
       :headers {"Date" (times/current-date), "Expires" expiration-date}}
      ; else (no if-modified-since header)
      (let [img-url (str img-root "/msl-raw-images/" *)
            img-bytes (-> (http/get img-url {:as :byte-array}) :body)]
        {:status 200
         :headers {"Content-Type" "image/jpg"
                   "Expires" expiration-date
                   "Last-Modified" (times/current-date)}
         :body (java.io.ByteArrayInputStream. img-bytes)})))
  (GET "/stereo" [& params]
    (stereo params))
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
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body (index parsed-params)
       :cookies (-> (select-keys params
                                 [:per-page :sorting :stereo :thumbs :view])
                  (assoc :visit-last visit-last
                         :visit-start visit-start
                         :visit-recent visit-recent
                         :cams (->> (:cams parsed-params)
                                 (map name)
                                 (str/join " ")))
                  set-expires)}))
  (route/resources "/")
  (route/not-found "You have wandered into a maze of twisty passages, all alike"))

(def ogler-handler
  (-> ogler-routes
    wrap-keyword-params
    wrap-params
    wrap-cookies))
