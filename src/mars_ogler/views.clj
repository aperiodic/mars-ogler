(ns mars-ogler.views
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clojure.string :as str]
            [mars-ogler.scrape :as scrape]
            [mars-ogler.times :as times])
  (:use [hiccup core page]))

(defn pic->html
  [{:keys [cam cam-name id lag released size sol taken-marstime taken-utc
           thumbnail-url type url w h]}]
  (html
    [:div.pic-wrapper
     [:div.pic
      [:a {:href url} [:img {:src thumbnail-url}]]]
     [:div.pic-info
      "Taken by " [:span.cam cam-name] " at " [:span.marstime taken-marstime]
      " local time on Sol " sol " (" [:span.takendate taken-utc] ")" [:br]
      "Released at " [:span.releasedate released] ", " lag " later" [:br]
      w [:span.x "x"] h " pixels, Type " type ", ID: " id]]))

(defn pics
  [{:keys [cams page per-page sorting thumbs]
    :or {cams "mahli mastcam navcam"
         page "1"
         per-page "25"
         sorting "released"
         thumbs "no"}}]
  (let [page (Integer/parseInt page)
        per-page (min (Integer/parseInt per-page) 100)
        sorting (keyword sorting)
        cams (->> (str/split cams #" ") (map keyword) set)
        cam-pred (fn [img] (-> img :cam cams))
        size-pred (case thumbs
                    "no" #(not= (:size %) :thumbnail)
                    "yes" (constantly true)
                    "only" #(= (:size %) :thumbnail))]
    (->> (filter (every-pred size-pred cam-pred)
                 (get @scrape/sorted-images sorting ()))
      (drop (* (dec page) per-page))
      (take per-page)
      (map pic->html))))
