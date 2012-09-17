(ns mars-ogler.views
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clojure.string :as str]
            [mars-ogler.scrape :as scrape]
            [mars-ogler.times :as times])
  (:use [hiccup core page]))

(def images (scrape/cached-images))

(defn parse-dates
  [image]
  (let [strs (select-keys image times/types)
        dates (into {} (for [[type date-str] strs]
                         [type (times/rfc-parser date-str)]))]
    (merge image dates)))

(defn unparse-dates
  [image]
  (let [dates (select-keys image times/types)
        strs (into {} (for [[type date] dates]
                         [type (times/rfc-printer date)]))]
    (merge image strs)))

(defn sort-images
  [images]
  (into {} (for [time-type times/types]
             (let [dated (map parse-dates images)
                   resort (case time-type
                            :released reverse
                            :taken-marstime identity
                            :taken-utc reverse)
                   sorted (-> (sort-by time-type dated) resort)
                   str-dated (map unparse-dates sorted)]
               [time-type (doall str-dated)]))))

(def sorted-images (sort-images images))

(defn filter-pics
  [{:keys [cams page per-page sorting thumbs]
    :or {cams "mahli mastcam navcam"
         page "1"
         per-page "25"
         sorting "released"
         thumbs "no"}}]
  (let [page (Integer/parseInt page)
        per-page (Integer/parseInt per-page)
        sorting (keyword sorting)
        cams (->> (str/split cams #" ") (map keyword) set)
        cam-pred (fn [img] (-> img :cam cams))
        size-pred (case thumbs
                    "no" #(not= (:size %) :thumbnail)
                    "yes" (constantly true)
                    "only" #(= (:size %) :thumbnail))
        filtered (filter (every-pred size-pred cam-pred)
                         (get sorted-images sorting))]
    (->> filtered
      (drop (* (dec page) per-page))
      (take per-page))))
