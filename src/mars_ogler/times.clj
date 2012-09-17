(ns mars-ogler.times
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clj-time.format :as fmt-time]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as html]))
  (def % partial)

(def types #{:released :taken-marstime :taken-utc})

(def rfc-format (fmt-time/formatters :rfc822))
(def rfc-printer (% fmt-time/unparse rfc-format))
(def rfc-parser (% fmt-time/parse rfc-format))

(def marstime-format (fmt-time/formatter "hh:mm:ss a"))
(def marstime-parser (% fmt-time/parse marstime-format))

(def utc-format (fmt-time/formatter "YYYY MMM dd HH:mm:ss"))
(def utc-parser (comp (% fmt-time/parse utc-format) str/lower-case))

(defn cell->marstime-str
  [cell]
  (-> cell
    html/text str/trim (str/replace #"\." "") marstime-parser rfc-printer))

(defn cell->utc-date-str
  [cell]
  (-> cell
    html/text str/trim utc-parser rfc-printer))
