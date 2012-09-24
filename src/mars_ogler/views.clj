(ns mars-ogler.views
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clojure.string :as str]
            [mars-ogler.cams :as cams]
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
     cam-name [:span.minor " at "] [:span.marstime taken-marstime]
     [:span.minor " on "] "Sol " sol]
    "Earth Date: &nbsp;" [:span.takendate taken-utc] [:br]
    "Released " lag " later at " [:span.releasedate released] [:br]
    w [:span.x " x "] h " " type " | ID: " id]])

(defn parse-params
  [{:keys [cams page per-page sorting thumbs]
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
     :thumbs thumbs'}))

(defn pics
  [{:keys [cams page per-page sorting thumbs]}]
  (let [cam-pred (fn [img] (-> img :cam cams))
        size-pred (case thumbs
                    :no #(not= (:size %) :thumbnail)
                    :yes (constantly true)
                    :only #(= (:size %) :thumbnail))]
    (->> (filter (every-pred size-pred cam-pred)
                 (get @scrape/sorted-images sorting ()))
      (drop (* (dec page) per-page))
      (take per-page)
      (map pic->hiccup))))

(defn toolbar
  [{:keys [cams page per-page sorting thumbs]}]
  (let [cam-boxes (for [cam [:hazcam :navcam :mastcam :mahli :mardi :chemcam]]
                    [:span.option
                     [:label
                      [:input {:type "checkbox", :name "cams", :value cam
                               :checked (cams cam)}
                       (cams/cam-names-by-cam cam)]]])
        sorting-names {:released "Date Released", :taken-utc "Date Taken"
                       :taken-marstime "Martian Time Taken"}
        sorting-buttons (for [sort-type [:released :taken-utc :taken-marstime]]
                          [:span.option
                           [:label
                            [:input {:type "radio"
                                     :name "sorting"
                                     :value sort-type
                                     :checked (= sort-type sorting)}]
                            (sorting-names sort-type)]])]
    [:div#toolbar-wrapper
     [:div#toolbar.content
      [:form {:action "/"}
       [:div#update [:input {:type "submit" :value "Update"}]]
       [:div#cam-toggles [:span.tool-label "Cameras:"] cam-boxes]
       [:div#sorting [:span.tool-label "Sort By:"] sorting-buttons]
       [:div#misc
        [:span.tool-label "Photos per Page:"]
        [:select {:name "per-page"}
         (for [amount [10 25 50 100]]
           [:option {:value amount, :selected (= amount per-page)}
            amount])]
        " | "
        [:span.tool-label "Thumbnails?:"]
        (for [opt [:no :yes :only]]
          [:span.option
           [:label
            [:input {:type "radio", :name "thumbs", :value opt,
                     :checked (= opt thumbs)}]
            (-> opt name str/capitalize)]])]]]]))

(defn index
  [filter-params]
  (let [filter-params (parse-params filter-params)]
    (html5
      [:head
       [:title "The Mars Ogler"]
       (include-css "/css/main.css")
       (include-css "http://fonts.googleapis.com/css?family=Oswald:400,700,300")
       (include-css "http://fonts.googleapis.com/css?family=Source+Sans+Pro:400,700")]
      [:body
       [:div#top-content.content
        [:h1#title "The Mars Ogler"]
        [:div#blurb
         "A Curiosity Mars Science Laboratory raw images viewer"
         [:br]
         "Built on top of "
         [:a {:href "http://curiositymsl.com/"} "Curiosity MSL Viewer"]
         " and "
         [:a {:href "http://mars.jpl.nasa.gov/"} "NASA Mars Exploration"]
         [:br]
         "How about a "
         [:a {:href "http://www.penny4nasa.org/the-mission/"} "Penny for NASA"]
         "?"
         ]]
       (toolbar filter-params)
       [:div#pics.content (pics filter-params)]
       ])))
