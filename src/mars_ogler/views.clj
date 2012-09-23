(ns mars-ogler.views
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clojure.string :as str]
            [mars-ogler.scrape :as scrape]
            [mars-ogler.times :as times])
  (:use [hiccup core page]))

(defn pic->hiccup
  [{:keys [cam cam-name id lag released size sol taken-marstime taken-utc
           thumbnail-url type url w h]}]
  [:div.pic-wrapper
   [:div.pic
    [:a {:href url} [:img {:src thumbnail-url}]]]
   [:div.pic-info
    [:div.title-line
     cam-name [:span.at " at "] [:span.marstime taken-marstime] " on Sol " sol]
    "Earth Date: &nbsp;" [:span.takendate taken-utc] [:br]
    "Released " lag " later at " [:span.releasedate released] [:br]
    w [:span.x " x "] h " pixels | Type " type " | ID: " id]])

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
      (map pic->hiccup))))

(defn index
  [filter-params]
  (html5
    [:head
     [:title "The Mars Ogler"]
     (include-css "/css/main.css")
     (include-css "http://fonts.googleapis.com/css?family=Oswald:400,700,300")
     (include-css "http://fonts.googleapis.com/css?family=Source+Sans+Pro:400,700")]
    [:body
     [:div#content
      [:h1 "The Mars Ogler"]
      [:div#blurb
       "A Curiosity Mars Science Laboratory raw images viewer."
       [:br]
       "Built on top of "
       [:a {:href "http://curiositymsl.com/"} "Curiosity MSL Viewer"]
       " and "
       [:a {:href "http://mars.jpl.nasa.gov/"} "NASA JPL Mars Exploration"]
       "."
       ]
      [:div#toolbar
       "Eventually there will be some tools here"]
      [:div#pics (pics filter-params)]
      ]]))
