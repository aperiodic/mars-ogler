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

(defn filter-pics
  [{:keys [cams sorting thumbs]}]
  (let [cam-pred (fn [img] (-> img :cam cams))
        size-pred (case thumbs
                    :no #(not= (:size %) :thumbnail)
                    :yes (constantly true)
                    :only #(= (:size %) :thumbnail))]
    (filter (every-pred size-pred cam-pred)
            (get @scrape/sorted-images sorting ()))))

(defn page-pics
  [pics {:keys [page per-page]}]
  (->> pics
    (drop (* (dec page) per-page))
    (take per-page)
    (map pic->hiccup)))

(defn page-link
  [page rest-qstring]
  (if (= rest-qstring "")
    (str "/?page=" page)
    (str "/?" rest-qstring "&page=" page)))

(defn page-links
  [pics {:keys [cams page per-page sorting thumbs ^String query-string]}]
  (let [last-page (-> (count pics) (quot per-page) inc)
        page-eq (str "page=" page)
        rest-qstring (if (.startsWith query-string "pa")
                       (.replace query-string page-eq "")
                       (.replace query-string (str "&" page-eq) ""))
        page-link #(page-link % rest-qstring)
        window 3
        missing-l (if (<= page (+ window 2))
                    (- (+ window 2) (dec page))
                    0)
        missing-r (if (>= (+ page window 2) last-page)
                    (- (+ window 2) (- last-page page))
                    0)
        left-lim (max 1 (- page (+ window missing-r)))
        right-lim (min last-page (+ page window missing-l))
        visible (->> (concat [1] (range left-lim (inc right-lim)) [last-page])
                  (filter (every-pred pos? #(<= % last-page)))
                  distinct)
        partitioned (loop [is visible, is' [], last-i nil]
                      (if-let [i (first is)]
                        (if-not last-i
                          (recur (rest is) (conj is' i) i)
                          (if (= (dec i) last-i)
                            (recur (rest is) (conj is' i) i)
                            (recur (rest is) (-> is' (conj "...") (conj i)) i)))
                        ; else (is empty)
                        is'))]
    [:div.pages.content {}
     [:form {:action (str "/?" rest-qstring)}
      [:div#goto
       [:input {:type "hidden" :name "thumbs" :value thumbs}]
       [:input {:type "hidden" :name "sorting" :value sorting}]
       [:input {:type "hidden" :name "per-page" :value per-page}]
       (for [cam cams]
         [:input {:type "hidden" :name "cams" :value cam}])
       [:span#goto-label "Go to "]
       [:select {:name "page"}
        (for [i (range 1 (inc last-page))]
          [:option {:value i, :selected (= i page)} i])]
       [:input {:type "submit" :value "Go!"}]]]
     (if (= page 1)
       [:div.page.limit.inactive "&lt;&lt; Prev"]
       [:a {:href (page-link (dec page))} [:div.page.limit "&lt;&lt; Prev"]])
     (for [i partitioned]
       (if (not (number? i))
         [:div.page.notlink i]
         (if (= i page)
           [:div.page.inactive i]
           [:a {:href (page-link i)} [:div.page i]])))
     (if (= page last-page)
       [:div.page.limit.inactive "Next &gt;&gt;"]
       [:a {:href (page-link (inc page))} [:div.page.limit "Next &gt;&gt;"]])
     ]))

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
           [:option {:value amount, :selected (= amount per-page)} amount])]
        " | "
        [:span.tool-label "Thumbnail Photos?"]
        (for [opt [:no :yes :only]]
          [:span.option
           [:label
            [:input {:type "radio", :name "thumbs", :value opt,
                     :checked (= opt thumbs)}]
            (-> opt name str/capitalize)]])]]]]))

(defn index
  [filter-params]
  (let [filter-params (parse-params filter-params)
        filtered-pics (filter-pics filter-params)
        pages (page-links filtered-pics filter-params)]
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
       (assoc pages 1 {:id "pages-top"})
       [:div#pics.content (page-pics filtered-pics filter-params)]
       (assoc pages 1 {:id "pages-bottom"})
       [:div#footer.content
        "Built by Dan Lidral-Porter. The Mars Ogler is "
        [:a {:href "http://www.gnu.org/philosophy/free-sw.html"} "free software"]
        "; go check out its "
        [:a {:href "https://github.com/aperiodic/mars-ogler"} "source code"] "."
        [:br]
        "Did you know NASA's ability to launch missions like Curiosity is "
        [:a {:href "http://www.planetary.org/get-involved/be-a-space-advocate/"}
         "being threatened by budget cuts"] "?"]])))
