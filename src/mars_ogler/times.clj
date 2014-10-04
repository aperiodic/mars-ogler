(ns mars-ogler.times
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clj-time.format :as fmt-time]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as html]))
  (def % partial)

(def types #{:acquired :released :taken-marstime :taken-utc})

(def rfc-format (fmt-time/formatters :rfc822))
(def rfc-parser (% fmt-time/parse rfc-format))
(def rfc-printer (% fmt-time/unparse rfc-format))

(defn current-date []
  (fmt-time/unparse rfc-format (time/now)))

(def marstime-in-format (fmt-time/formatter "HH:mm:ss a 'LMST'"))
(def marstime-parser (% fmt-time/parse marstime-in-format))

(def marstime-out-format
  (fmt-time/formatter "h:mm a"))
(def marstime-printer (% fmt-time/unparse marstime-out-format))

(def marstime-out-pretty-format
  (fmt-time/formatter "h:mm <'span class=\"meridian\"'>a</'span'>"))
(def marstime-pretty-printer (% fmt-time/unparse marstime-out-pretty-format))

(def utc-in-format (fmt-time/formatter "YYYY MMM dd HH:mm:ss 'UTC'"))
(def utc-parser (% fmt-time/parse utc-in-format))

(def utc-out-format
  (fmt-time/formatter "h:mm a 'UTC' d MMMM YYYY"))
(def utc-printer (% fmt-time/unparse utc-out-format))

(def utc-out-pretty-format
  (fmt-time/formatter (str "h:mm <'span class=\"meridian\"'>a</'span'> 'UTC' "
                           "d MMMM YYYY")))
(def utc-pretty-printer (% fmt-time/unparse utc-out-pretty-format))

(def date-format (fmt-time/formatter "d MMMM YYYY"))
(def date-printer (% fmt-time/unparse date-format))

(def time-format (fmt-time/formatter "hh:mm a"))
(def time-printer (% fmt-time/unparse time-format))

(defn- format-with-units
  ([value units]
   (let [s (str value " " units)]
     (if (= value 1)
       (.substring s 0 (dec (count s)))
       s)))
  ([v1 u1 v2 u2]
   (let [s1 (format-with-units v1 u1)]
     (if (= v2 0)
       s1
       (str s1 " and " (format-with-units v2 u2))))))

(defn format-interval
  [interval]
  (let [secs (time/in-secs interval)
        mins (time/in-minutes interval)
        hours (time/in-hours interval)
        days (time/in-days interval)]
    (cond
      (< secs 60) (format-with-units secs "seconds")
      (< mins 60) (format-with-units mins "minutes")
      (< hours 24) (format-with-units hours "hours" (mod mins 60) "minutes")
      :otherwise (format-with-units days "days" (mod hours 24) "hours"))))
