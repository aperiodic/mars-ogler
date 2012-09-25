(ns mars-ogler.scrape
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clj-time.format :as fmt-time]
            [mars-ogler.cams :as cams]
            [mars-ogler.times :as times]
            [net.cgrand.enlive-html :as html]))
  (def % partial)

;;
;; Scrapin'
;;

(def cell->int (comp #(Integer/parseInt %) html/text))

(defn label->cell-type
  [label]
  (-> label (str/split #" ") first keyword))

(defmulti cell->map
  (fn [label _] (label->cell-type label)))

(defmethod cell->map :camera
  [_ cell]
  (let [abbrev (-> (html/text cell) str/trim)]
    {:cam (get cams/cams-by-abbrev abbrev abbrev)
     :cam-name (get cams/cam-names-by-abbrev abbrev abbrev)}))

(defmethod cell->map :delay
  [_ cell]
  {:delay (cell->int cell)})

(defmethod cell->map :height
  [_ cell]
  {:h (cell->int cell)})

(defmethod cell->map :lag
  [_ _]
  {})

(defmethod cell->map :lmst
  [_ cell]
  {:taken-marstime (times/cell->marstime-str cell)})

(defmethod cell->map :name
  [_ cell]
  {:id (-> (html/text cell) str/trim)
   :url (-> (html/select cell [:a])
          first
          (get-in [:attrs :href]))})

(defmethod cell->map :released
  [_ cell]
  {:released (times/cell->utc-date-str cell)})

(defmethod cell->map :sol
  [_ cell]
  {:sol (cell->int cell)})

(defmethod cell->map :taken
  [_ cell]
  {:taken-utc (times/cell->utc-date-str cell)})

(defmethod cell->map :thumbnail
  [_ cell]
  {:thumbnail-url (-> (html/select cell [:img])
                    first
                    (get-in [:attrs :src]))})

(defmethod cell->map :type
  [_ cell]
  {:type (-> (html/text cell) str/trim)})

(defmethod cell->map :width
  [_ cell]
  {:w (cell->int cell)})

(defn classify-size
  [{:keys [w h type] :as image}]
  (let [long-dim (max w h)
        size (cond
               (or (= type "I") (= type "T")) :thumbnail
               (>= long-dim 512) :large
               (>= long-dim 256) :medium
               :otherwise :small)]
    (assoc image :size size)))

(defn row->url
  [row]
  (let [img-link (-> (html/select row [:a]) first)]
    (get-in img-link [:attrs :href])))

(defn row->map
  [labels row]
  (let [cell-data (->> (html/select row [:td])
                    (map cell->map labels)
                    (apply merge))
        row-data {:url (row->url row)}]
    (merge cell-data row-data)))

(defn body->images
  [body-node]
  (let [img-table (-> (html/select body-node [:table]) last)
        [label-row & img-rows] (html/select img-table [:tr])
        labels (->> (html/select label-row [:th])
                 (map (comp str/trim html/text)))]
    (->> img-rows
      (map (% row->map labels))
      (map classify-size))))

;;
;; Actually Fetching the Pages
;;

(defn page-url
  ([] (page-url 1))
  ([i] (str "http://curiositymsl.com/?page=" i "&limit=100")))

(defn fetch-page
  [i]
  (html/html-resource (java.net.URL. (page-url i))))

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
      (assoc :lag lag))))

(defn unparse-dates
  [img]
  (let [dates (select-keys img times/types)]
    (into img (for [[type date] dates]
                [type (times/rfc-printer date)]))))

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
;; AOT Sorting
;;

(defn sort-images
  "The images must contain joda DateTime objects in their date keys."
  [imgs]
  (into {} (for [time-type times/types]
             (let [resort (case time-type
                            :released reverse
                            :taken-marstime identity
                            :taken-utc reverse)]
               [time-type (->> imgs
                            (sort-by time-type)
                            resort
                            (map format-image)
                            doall)]))))

;;
;; Transformations
;;

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
  (let [new-imgs (map parse-dates (fetch-new-images old-imgs))
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
