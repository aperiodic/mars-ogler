(ns mars-ogler.image-utils
  (:require [clojure.string :as str]
            [mars-ogler.cams :as cams]))

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
