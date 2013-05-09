(ns mars-ogler.image-utils
  (:require [clj-time.core :as time]
            [clojure.string :as str]
            [mars-ogler.cams :as cams]
            [mars-ogler.times :as times]))

;;
;; Camera Information
;;

(defn id->camera
  [id]
  (let [abbrev (re-find #"[A-Z]+" id)]
    {:cam (cams/abbrev->cam abbrev)
     :cam-name (cams/abbrev->name abbrev)}))

(defn image->camera
  [img]
  (-> img :id id->camera))

;;
;; Size
;;

(defn thumbnail?
  [img]
  (= (:type img) "Thumbnail"))

;;
;; URLs
;;

(def url-prefix "http://mars.jpl.nasa.gov/msl-raw-images/")
(def prefix-length (count url-prefix))

(defn image->url
  [img]
  (str url-prefix (:url img)))

(defn image->thumbnail-url
  [img]
  (str url-prefix (:thumbnail-url img)))

(defn truncate-url
  [url]
  (if (.startsWith ^String url url-prefix)
    (.substring url prefix-length)
    url))

(defn truncate-urls
  [img]
  (-> img
    (update-in [:url] truncate-url)
    (update-in [:thumbnail-url] truncate-url)))

;;
;; Formatting
;;

(defn image->lag
  [img]
  (let [{taken :taken-utc, released :released} img]
    (times/format-interval (if (time/before? taken released)
                             (time/interval taken released)
                             (time/interval taken taken)))))
(defn format-dates
  [img]
  (let [full-dates (select-keys img (disj times/types :taken-marstime))
        marstime (-> img :taken-marstime times/marstime-printer)]
    (-> img
      (assoc :lag (image->lag img), :taken-marstime marstime)
      (merge (into {} (for [[type date] full-dates]
                        [type (times/utc-printer date)]))))))

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
                         "Q" "Thumbnail"
                         "S" "Depth Map"
                         "T" "Thumbnail"
                         "U" "Depth Map"
                         (str "Type " type-code " Image"))]
    (assoc img :type formatted-type)))

(def format-image (comp format-dates format-type))
