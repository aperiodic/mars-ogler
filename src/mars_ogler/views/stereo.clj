(ns mars-ogler.views.stereo
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [mars-ogler.cams :as cams]
            [mars-ogler.scrape :as scrape]
            [mars-ogler.times :as times])
  (:use [hiccup core page]))

(defn pairwise-stereo-pair?
  [a b]
  (and (= (:taken-utc a) (:taken-utc b)) ; taken at the same time
       (= (:cam a) (:cam b)) ; from the same group of cameras
       (let [parities (map (comp cam-parity :cam-name) [a b])]
         (and (every? identity parities)   ; one is a left camera, the other is
              (not (apply = parities)))))) ; a right camera

(defn exhaustive-stereo-pair-search
  )
