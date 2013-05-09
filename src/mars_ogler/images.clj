(ns mars-ogler.images
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [mars-ogler.cams :as cams]
            [mars-ogler.times :as times])
  (:use [mars-ogler.image-utils :only [image->camera truncate-urls]]))

;;
;; Public API
;; Just some atoms containing the images arranged in various fashions
;;

(def ^{:doc "If you just want to grab the most recent images pre-sorted by all
            three orderings, then deref this atom."}
  sorted-images (atom nil))

(def indices (atom nil))

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
              times/format-interval)
        acquired (:acquired dates)
        acquired-stamp (if acquired
                         (cvt-time/to-long acquired)
                         0)]
    (-> (merge img dates)
      (assoc :acquired-stamp acquired-stamp))))

(defn unparse-dates
  [img]
  (let [dates (select-keys img times/types)]
    (-> (into img (for [[type date] dates]
                    [type (times/rfc-printer date)]))
      (dissoc :acquired-stamp))))

;;
;; Stereo Pairs
;;

(defn stereo-pair?
  [[a b]]
  (let [{a-cam :cam, a-cam-name :cam-name} (image->camera a)
        {b-cam :cam, b-cam-name :cam-name} (image->camera b)]
    (and (= (:taken-utc a) (:taken-utc b)) ; taken at the same time
       (= a-cam b-cam) ; from the same group of cameras
       (= (:w a) (:w b)) (= (:h a) (:h b)) ; they're the same size
       (let [parities (map cams/cam-parity [a-cam-name b-cam-name])]
         (and (every? identity parities)    ; one is a left camera, the other is
              (not (apply = parities))))))) ; a right camera

(defn mark-stereo-images
  [imgs]
  (loop [in (sort-by :taken-utc imgs), out []]
    (if (second in)
      (let [first-two (take 2 in)]
        (if (stereo-pair? first-two)
          (let [[a b] (map #(assoc % :stereo? true) first-two)]
            (recur (-> in rest rest) (-> out (conj a) (conj b))))
          ; else (first two imgs not a stero pair)
          (recur (rest in) (conj out (first in)))))
      ; else (one or zero images left)
      (concat out in))))

(defn stereo-partner-index
  [imgs]
  (let [pairs (filter stereo-pair? (->> imgs
                                     (sort-by :taken-utc) (partition 2 1)))
        first->id (comp :id first)
        second->id (comp :id second)]
    (merge (zipmap (map first->id pairs) (map second->id pairs))
           (zipmap (map second->id pairs) (map first->id pairs)))))

;;
;; Processing
;;
;; Do higher-level work on the scraped image maps before we sort & index them,
;; e.g. find stereo pairs.
;;

(defn process-images
  [imgs]
  (-> imgs mark-stereo-images))

;;
;; Arranging Images (Sorting & Indexing)
;;

(defn sort-images
  "The images must contain joda DateTime objects in their date keys."
  [imgs]
  (into {} (for [time-type (disj times/types :acquired)]
             (let [resort (case time-type
                            :released reverse
                            :taken-marstime identity
                            :taken-utc reverse)]
               [time-type (->> imgs
                            (sort-by time-type)
                            resort
                            doall)]))))

(defn index-images
  [imgs]
  {:id (zipmap (map :id imgs) imgs)
   :stereo (stereo-partner-index imgs)})

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
      (catch java.io.FileNotFoundException _))))

(defn cache-images!
  [imgs]
  (->> imgs
    (map unparse-dates)
    (map #(dissoc % :cam :cam-name :size))
    prn-str
    (spit $images-file)))

(defn setup!
  []
  (let [processed (-> (get-cached-images) process-images)
        indices' (future (index-images processed))
        sorted-images' (future (sort-images processed))]
    (reset! indices @indices')
    (reset! sorted-images @sorted-images')))

(defn update!
  [imgs]
  (cache-images! imgs)
  (let [processed (-> imgs process-images)
        indices' (future (index-images processed))
        sorted-images' (future (sort-images processed))]
    (reset! indices @indices')
    (reset! sorted-images @sorted-images')))
