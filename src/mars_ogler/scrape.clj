(ns mars-ogler.scrape
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clj-http.client :as http]
            [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clj-time.format :as fmt-time]
            [mars-ogler.cams :as cams]
            [mars-ogler.times :as times]
            [net.cgrand.enlive-html :as html]))
  (def % partial)

;;
;; Date Parsing/Unparsing
;;
;; Gotta store the dates as strings, but want them as Joda DateTime objects
;; to do sorting.
;;

(defn parse-dates
  [img]
  (let [strs (select-keys img times/types)
        dates (into {} (for [[type date-str] strs]
                         [type (times/rfc-parser date-str)]))
        taken (:taken-utc dates)
        released (:released dates)
        lag (-> (if (time/before? taken released)
                  (time/interval taken released)
                  (time/interval taken taken))
              times/format-interval)]
    (-> (merge img dates)
      (assoc :lag lag, :released-stamp (cvt-time/to-long released)))))

(defn unparse-dates
  [img]
  (let [dates (select-keys img times/types)]
    (-> (into img (for [[type date] dates]
                    [type (times/rfc-printer date)]))
      (dissoc :lag :released-stamp))))

;;
;; Scrapin'
;;

(defn id->camera
  [id]
  (let [abbrev (re-find #"[A-Z]+" id)]
    {:cam (cams/abbrev->cam abbrev)
     :cam-name (cams/abbrev->name abbrev)}))

(defn id->type
  [id]
  (let [mast-mahli-mardi #"([A-Z])\d_"
        nav-chem-haz #"_([A-Z])"]
    (if-let [type (second (re-find mast-mahli-mardi id))]
      type
      (second (re-find nav-chem-haz id)))))

(defn div->times
  [div]
  (let [[marstime taken released] (-> div
                                    (html/select [:.rawtable]), first
                                    (html/select [:tr]), second
                                    (html/select [:td])
                                    (->> (map #(-> % :content second str/trim))))]
    [(-> marstime (str/replace #"\." "") times/marstime-parser times/rfc-printer)
     (-> taken times/utc-parser times/rfc-printer)
     (-> released times/utc-parser times/rfc-printer)]))

(defn div->map
  [div]
  (let [rawtable (-> (html/select div [:.rawtable]) first)
        id (-> (html/select rawtable [:a]) first html/text str/trim)
        {:keys [cam cam-name]} (id->camera id)
        type (id->type id)
        sol (->> (html/text rawtable) (re-find #"sol (\d+)") second Integer.)
        [marstime taken-utc released] (div->times div)
        thumbnail-url (-> (html/select div [:.thumbnail :a :img])
                        first :attrs :src)
        url (-> (html/select div [:.rawtable :a]) first :attrs :href)
        dims (->> (re-find #"(\d+)x(\d+)" (html/text div)) (drop 1))
        [w h] (for [dim dims] (Integer/parseInt dim))]
    {:w w, :h h, :id id, :cam cam, :cam-name cam-name :type type, :sol sol
     :taken-marstime marstime :taken-utc taken-utc, :released released
     :thumbnail-url thumbnail-url, :url url}))

(defn classify-size
  [{:keys [w h type] :as image}]
  (let [long-dim (max w h)
        size (cond
               (or (= type "I") (= type "T") (= type "Q")) :thumbnail
               (>= long-dim 1024) :large
               (>= long-dim 512) :medium
               :otherwise :small)]
    (assoc image :size size)))

(defn body->images
  [body-node]
  (let [image-divs (html/select body-node [:body :> :div])]
    (->> image-divs
      (map div->map)
      (map classify-size)
      (map parse-dates))))

;;
;; Actually Fetching the Pages
;;

(defn fetch-page
  [i]
  (let [page (:body (http/post
                      "http://curiositymsl.com/localphp/loadmore.php"
                      {:headers {"Cookie" "PHPSESSID=3v55ibi7c0l25836c2chenpmo1;"
                                 "User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/537.13 (KHTML, like Gecko) Chrome/24.0.1290.1 Safari/537.13"
                                 "X-Requested-With" "XMLHttpRequest"}
                       :form-params {:q "", :z "UTC", :s "etreleased",
                                     :l "all" :o "desc", :n 100
                                     :start (* (dec i) 100)}}))
        tmp-file "/tmp/ogle-scrape"]
    (spit tmp-file page)
    (-> (html/html-resource (java.io.File. tmp-file)) first)))

(defn fetch-new-images
  [known-imgs]
  (let [seen? (fn [img] (some #(= (:id %) (:id img)) (take 100 known-imgs)))]
    (println "Checking for new photos...")
    (loop [i 1, imgs ()]
      (let [page-imgs (body->images (fetch-page i))
            last-page? (some seen? page-imgs)
            imgs' (concat imgs (take-while (complement seen?) page-imgs))]
        (if (or last-page? (empty? page-imgs))
          imgs'
          (do
            (Thread/sleep 500) ; ratelimit ourselves
            (println "Oh boy, there's a lot! Fetching page" (inc i))
            (recur (inc i) imgs')))))))

;;
;; Formatting
;;

(defn format-dates
  [img]
  (let [full-dates (select-keys img (disj times/types :taken-marstime))]
    (merge
      img
      {:taken-marstime (-> img :taken-marstime times/marstime-printer)}
      (into {} (for [[type date] full-dates]
                 [type (times/utc-printer date)])))))

(defn format-type
  [img]
  (let [type-code (:type img)
        formatted-type (condp = type-code
                         "B" "Subsampled Image"
                         "C" "Subsampled Image"
                         "D" "Subsampled Image"
                         "E" "Full Image"
                         "F" "Full Image"
                         "I" "Thumbnail"
                         "S" "Depth Map"
                         "T" "Thumbnail"
                         "U" "Depth Map"
                         (str "Type " type-code " Image"))]
    (assoc img :type formatted-type)))

(def format-image (comp format-dates format-type))

;;
;; Prep Images for Webserver
;;

(def blacklist
  "Vector of ID-intervals (both endpoints inclusive), ordered by release date."
  [["0064ML0327000276M0_DXXX", "0064ML0327000309M0_DXXX"]
   ["0064ML0327000031M0_DXXX", "0064ML0327000306M0_DXXX"]])

(defn apply-blacklist
  [imgs blacklist]
  (loop [in imgs, out [], bl-pos [0 0]]
    (if (empty? in)
      out
      (let [img (first in)
            [bl-interval bl-endpoint] bl-pos]
        (if (= (:id img) (get-in blacklist bl-pos))
          (recur (rest in) out (if (zero? bl-endpoint)
                                 [bl-interval 1]
                                 [(inc bl-interval) 0]))
          ;; else (img is not a blacklist interval endpoint)
          (if (zero? bl-endpoint)
            (recur (rest in) (conj out img) bl-pos)
            (recur (rest in) out bl-pos)))))))

(defn de-dupe
  [imgs]
  (loop [in imgs, out [], seen #{}]
    (if-let [img (first in)]
      (if (contains? seen (:id img))
        (recur (rest in) out seen)
        (recur (rest in) (conj out img) (conj seen (:id img))))
      out)))

(defn sort-images
  "The images must contain joda DateTime objects in their date keys."
  [imgs]
  (let [prepped (-> imgs de-dupe (apply-blacklist blacklist))]
    (into {} (for [time-type times/types]
               (let [resort (case time-type
                              :released reverse
                              :taken-marstime identity
                              :taken-utc reverse)]
                 [time-type (->> prepped
                              (sort-by time-type)
                              resort
                              (map format-image)
                              doall)])))))

;;
;; State & Main
;;

(def $images-file "cache/images")

(defn get-cached-images
  "Read the images cached in the `$images-file`."
  []
  (binding [*read-eval* false]
    (try (->> $images-file
           slurp
           read-string
           (map parse-dates))
      (catch java.io.FileNotFoundException _ ))))

(defn cache-images!
  [imgs]
  (->> imgs
    (map unparse-dates)
    prn-str
    (spit $images-file)))

(defn print-summary
  [new-images all-images]
  (if (empty? new-images)
  (println "I didn't find any new photos, so we still have"
           (count all-images) "total")
  (println "I found" (count new-images) "new photos, so now we have"
             (count all-images) "total")))

(defn fetch-tick
  [old-imgs]
  (let [new-imgs (fetch-new-images old-imgs)
        all-imgs (concat new-imgs old-imgs)]
    {:all all-imgs, :new new-imgs, :old old-imgs}))

(def ^{:doc "If you just want to grab the most recent images pre-sorted by all
            three orderings, then deref this atom."}
  sorted-images (atom nil))

(defn setup-state!
  []
  (reset! sorted-images (sort-images (get-cached-images))))

(defn update-states!
  [imgs]
  (cache-images! imgs)
  (reset! sorted-images (sort-images imgs)))

(defn scrape-loop!
  []
  (let [backup-imgs (atom (get-cached-images))]
    (while true
      (try (loop [imgs @backup-imgs]
             (let [{:keys [new old all]} (fetch-tick imgs)]
               (print-summary new all)
               (when-not (empty? new)
                 (update-states! all)
                 (reset! backup-imgs all))
               (Thread/sleep (* 60 1000))
               (recur all)))
        (catch Throwable err
          (let [cause (.getCause err)
                err-msg (str (.getMessage err)
                             (and cause
                                  (str " caused by: ") (.getMessage cause)))]
            (println "Had some trouble in the scrape loop:" err-msg))
          (Thread/sleep (* 60 1000)))))))
