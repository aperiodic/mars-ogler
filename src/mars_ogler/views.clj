(ns mars-ogler.views
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clojure.string :as str]
            [mars-ogler.cams :as cams]
            [mars-ogler.scrape :as scrape]
            [mars-ogler.times :as times])
  (:use [hiccup core page]))

(defn pic->list-hiccup
  [{:keys [acquired-stamp cam cam-name id lag released size sol taken-marstime
           taken-utc thumbnail-url type url w h]}
   visit-last]
  (let [new? (> acquired-stamp visit-last)]
    [:div.list-pic-wrapper
     [:div.pic.list-pic
      [:a {:href url}
       [:img {:src thumbnail-url
              :class (if new? "new pic-img" "pic-img")}]]]
     [:div.pic-info
      [:div.title-line
       cam-name [:span.minor " at "] [:span.marstime taken-marstime]
       [:span.minor " on "] "Sol " sol]
      "Earth Date: &nbsp;" [:span.takendate taken-utc] [:br]
      "Released " lag " later at " [:span.releasedate released]
      (when new? " (New)") [:br]
      w [:span.x " x "] h " " type " " "| ID: " [:a {:href url} id]]]))

(defn pic->grid-hiccup
  [{:keys [acquired-stamp cam-name id sol taken-marstime thumbnail-url url]}
   visit-last]
  (let [new? (> acquired-stamp visit-last)
        clean-lmst (str (first (re-find #"^((\d){1,2}:\d\d)" taken-marstime))
                        " "
                        (first (re-find #"(PM|AM)" taken-marstime)))
        label (str clean-lmst " on Sol " sol "\n" id)]
    [:div.grid-pic-wrapper
     [:div.pic.grid-pic
      [:a {:href url, :title label}
       [:img {:class (if new? "new pic-img" "pic-img")
              :src thumbnail-url
              :alt (str cam-name " at " label)}]]]]))

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
  [pics {:keys [page per-page view visit-last]}]
  (->> pics
    (drop (* (dec page) per-page))
    (take per-page)
    (map (if (= view :grid)
           #(pic->grid-hiccup % visit-last)
           #(pic->list-hiccup % visit-last)))))

(defn page-link
  [page rest-qstring]
  (if (= rest-qstring "")
    (str "/?page=" page)
    (str "/?" rest-qstring "&page=" page)))

(defn page-links
  [pics {:keys [cams page per-page sorting thumbs ^String query-string view]}]
  (let [last-page (-> (count pics) (quot per-page) inc)
        page-eq (str "page=" page)
        rest-qstring (if (.startsWith query-string "pa")
                       (.replace query-string page-eq "")
                       (.replace query-string (str "&" page-eq) ""))
        page-link #(page-link % rest-qstring)
        grid? (= view :grid)
        window (if grid? 4 3)
        missing-l (-> (if (<= page (+ window 2))
                        (- (+ window 2) (dec page))
                        0)
                    (+ (if grid? 1 0)))
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
    [:div {}
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
       [:a {:href (page-link (inc page))} [:div.page.limit "Next &gt;&gt;"]])]))

(defn toolbar
  [{:keys [cams page per-page sorting thumbs view]}]
  (let [grid? (= view :grid)
        sorting-names {:released "Date Released", :taken-utc "Date Taken"
                       :taken-marstime "Time of Martian Day Taken"}]
    [:div#toolbar-wrapper
     [:div#toolbar {:class (str "content "(if grid? "grid-content"))}
      [:form {:action "/"}
       [:input {:type "hidden", :name "page", :value page}]
       [:div.update {:id (if grid? "grid-update" "update")}
        [:input {:type "submit" :value "Update"}]]
       [:div#cam-toggles
        [:span.tool-label "Cameras:"]
        (for [cam [:hazcam :navcam :mastcam :mahli :mardi :chemcam]]
          [:span.option
           [:label [:input {:type "checkbox", :name "cams", :value cam
                            :checked (cams cam)}]
            (cams/cam->name cam)]])]
       [:div#sorting
        [:span.tool-label "Sort By:"]
        (for [sort-type [:released :taken-utc :taken-marstime]]
          [:span.option
           [:label [:input {:type "radio"
                            :name "sorting"
                            :value sort-type
                            :checked (= sort-type sorting)}]
            (sorting-names sort-type)]])]
       [:div#thumbnails
        [:span.tool-label "Show Thumbnail Photos?"]
        (for [opt [:no :yes :only]]
          [:span.option
           [:label [:input {:type "radio", :name "thumbs", :value opt,
                            :checked (= opt thumbs)}]
            (-> opt name str/capitalize)]])]
       [:div#view
        [:span.tool-label "Photos per Page:"]
        [:select {:name "per-page"}
         (for [amount (if grid? [25 50 100 200] [10 25 50 100])]
           [:option {:value amount, :selected (= amount per-page)} amount])]
        " | &nbsp;"
        [:span.tool-label "View as:"]
        (for [view-type [:list :grid]]
          [:span.option
           [:label [:input {:type "radio"
                     :name "view"
                     :value view-type
                     :checked (= view-type view)}]
            (-> view-type name str/capitalize)]])]]]]))

(defn index
  [{:keys [view] :as params}]
  (let [filtered-pics (filter-pics params)
        last-visit (:visit-last params)
        new-count (-> (take-while
                        #(> (:acquired-stamp %) last-visit)
                        (filter-pics (assoc params :sorting :released)))
                    count)
        pages (page-links filtered-pics params)
        grid? (= view :grid)]
    (html5
      [:head
       [:title "The Mars Ogler"]
       [:link {:rel "shortcut icon" :href "/favicon.ico"}]
       (include-css "/css/main.css")
       (include-css "http://fonts.googleapis.com/css?family=Oswald:700")
       (include-css "http://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400")]
      [:body
       [:div#top-content {:class (str "content " (if grid? "grid-content"))}
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
         "?"]]
       (toolbar params)
       (assoc pages 1
              {:id "pages-top"
               :class (str "pages content " (if grid? "grid-content"))})
       [:div#pics {:class (str "content " (if grid? "grid-content"))}
        (when (> new-count 0)
          [:div#new-count
           "Rad, there are " new-count " " [:span.new "new"] " photos!"])
        (page-pics filtered-pics params)]
       (assoc pages 1
              {:id "pages-bottom"
               :class (str "pages content " (if grid? "grid-content"))})
       [:div {:id (if grid? "grid-footer" "footer")
              :class (str "content footer " (if grid? "grid-content"))}
        "Built by Dan Lidral-Porter. The Mars Ogler is "
        [:a {:href "http://www.gnu.org/philosophy/free-sw.html"} "free software"]
        "; go check out its "
        [:a {:href "https://github.com/aperiodic/mars-ogler"} "source code"] "."
        [:br]
        "Did you know NASA's ability to launch missions like Curiosity is "
        [:a {:href "http://www.planetary.org/get-involved/be-a-space-advocate/"}
         "being threatened by budget cuts"] "?"]])))
