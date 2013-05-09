(ns mars-ogler.views.index
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clojure.string :as str]
            [mars-ogler.cams :as cams]
            [mars-ogler.images :as images]
            [mars-ogler.times :as times])
  (:use [hiccup core page]
        [mars-ogler.image-utils :only [format-image image->camera image->lag
                                       image->url image->thumbnail-url
                                       thumbnail?]]))

(def min-size->pred
  {:thumb (constantly true)
   :medium (fn [img] (not (thumbnail? img)))
   :full (fn [img]
           (let [cam-max-size (-> img
                                image->camera, :cam, cams/cam->max-size)]
             (or (>= (:w img) cam-max-size)
                 (>= (:h img) cam-max-size))))})

(defn filter-pics
  [{:keys [cams min-size sorting stereo thumbs]}]
  (let [cam-pred (fn [img] (cams (-> img image->camera :cam)))
        stereo-pred (if (= stereo :on)
                      :stereo?
                      (constantly true))]
    (filter (every-pred cam-pred stereo-pred (min-size->pred min-size))
            (get @images/sorted-images sorting ()))))

(defn pic->href
  [{:keys [id stereo?] :as pic}]
  (if-not stereo?
    (image->url pic)
    (let [partner-id (get-in @images/indices [:stereo id])
          [l-id r-id] (sort [id partner-id])]
      (str "/stereo?l_id=" l-id "&r_id=" r-id))))

(defn pic->list-hiccup
  [{:keys [acquired-stamp id lag released size sol stereo? taken-marstime
           taken-utc type w h]
    :as pic}
   visit-last]
  (let [new? (> acquired-stamp visit-last)
        cam-name (-> pic image->camera :cam-name)]
    [:div.list-pic-wrapper
     [:div.pic.list-pic
      [:a {:href (pic->href pic)}
       [:img {:src (image->thumbnail-url pic)
              :class (if new? "new pic-img" "pic-img")}]]]
     [:div.pic-info
      [:div.title-line
       cam-name [:span.minor " at "] [:span.marstime taken-marstime]
       [:span.minor " on "] "Sol " sol]
      "Earth Date: &nbsp;" [:span.takendate taken-utc] [:br]
      "Released " lag " later at " [:span.releasedate released]
      (when new? " (New)") [:br]
      w [:span.x " x "] h " " type " | ID: " [:a {:href (pic->href pic)} id]]]))

(defn pic->grid-hiccup
  [{:keys [acquired-stamp id sol taken-marstime] :as pic}
   visit-last]
  (let [new? (> acquired-stamp visit-last)
        cam-name (-> pic image->camera :cam-name)
        clean-lmst (str (first (re-find #"^((\d){1,2}:\d\d)" taken-marstime))
                        " "
                        (first (re-find #"(PM|AM)" taken-marstime)))
        label (str clean-lmst " on Sol " sol "\n" id)]
    [:div.grid-pic-wrapper
     [:div.pic.grid-pic
      [:a {:href (pic->href pic), :title label}
       [:img {:class (if new? "new pic-img" "pic-img")
              :src (image->thumbnail-url pic)
              :alt (str cam-name " at " label)}]]]]))

(defn page-pics
  [pics {:keys [page per-page view visit-last]}]
  (->> pics
    (drop (* (dec page) per-page))
    (take per-page)
    (map format-image)
    (map (if (= view :grid)
           #(pic->grid-hiccup % visit-last)
           #(pic->list-hiccup % visit-last)))))

(defn page-link
  [page rest-qstring]
  (if (= rest-qstring "")
    (str "/?page=" page)
    (str "/?" rest-qstring "&page=" page)))

(defn page-links
  [pics {:keys [cams page per-page sorting stereo
                thumbs ^String query-string view]}]
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
       [:input {:type "hidden" :name "per-page" :value per-page}]
       [:input {:type "hidden" :name "sorting" :value sorting}]
       [:input {:type "hidden" :name "stereo" :value stereo}]
       [:input {:type "hidden" :name "thumbs" :value thumbs}]
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
  [{:keys [cams min-size page per-page sorting stereo thumbs view]}]
  (let [grid? (= view :grid)
        sorting-names {:released "Date Released", :taken-utc "Date Taken"
                       :taken-marstime "Local Time Taken"}]
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
        [:select {:name "sorting"}
         (for [sort-type [:released :taken-utc :taken-marstime]]
           [:option {:value sort-type
                     :selected (= sort-type sorting)}
            (sorting-names sort-type)])]
        " &nbsp;"
        [:span.tool-label "Photos per Page:"]
        [:select {:name "per-page"}
         (for [amount (if grid? [25 50 100 200] [10 25 50 100])]
           [:option {:value amount, :selected (= amount per-page)} amount])]
        " &nbsp;"
        [:span.tool-label "View as:"]
        [:select {:name "view"}
         (for [view-type [:list :grid]]
          [:option {:value view-type, :selected (= view-type view)}
            (-> view-type name str/capitalize)])]]
       [:div#filters
        [:span.tool-label "Minimum Size:"]
        (let [size->name {:thumb "Thumbnail"
                          :medium "Medium"
                          :full "Full-Size"}]
          (for [size [:thumb :medium :full]]
            [:span.option
             [:label [:input {:type "radio"
                              :name "min-size"
                              :value size
                              :checked (= min-size size)}]
              (size->name size)]]))
        " | &nbsp;"
        [:label [:input {:name "stereo" :type "checkbox"
                         :checked (= :on stereo)}]
         "Only Show Stereo Pairs"]]
       ]]]))

(defn index
  [{:keys [view] :as params}]
  (let [filtered-pics (filter-pics params)
        last-visit (:visit-last params)
        new-count (count (take-while
                           #(> (:acquired-stamp %) last-visit)
                           (filter-pics (assoc params :sorting :released))))
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
