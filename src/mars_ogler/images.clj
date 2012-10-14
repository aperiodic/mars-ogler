(ns mars-ogler.images
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [mars-ogler.times :as times]))

;;
;; Public API
;; Just some atoms containing the images arranged in various fashions
;;

(def ^{:doc "If you just want to grab the most recent images pre-sorted by all
            three orderings, then deref this atom."}
  sorted-images (atom nil))

(def lookups (atom nil))

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
;; Sorting Images
;; Pre-sort them for the index view
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

(defn setup!
  []
  (reset! sorted-images (sort-images (get-cached-images))))

(defn update!
  [imgs]
  (cache-images! imgs)
  (reset! sorted-images (sort-images imgs)))
