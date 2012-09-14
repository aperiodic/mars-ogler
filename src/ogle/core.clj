(ns ogle.core
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clj-time.format :as fmt-time]
            [net.cgrand.enlive-html :as html])
  (:use [clojure.string :only [lower-case trim]]))
  (def % partial)

(defn page-url
  ([] (page-url 1))
  ([i] (str "http://curiositymsl.com/?page=" i "&limit=100")))

(def first-page (html/html-resource (java.net.URL. (page-url 1))))

(def label->attr {"camera" :cam
                  "height" :h
                  "lag (hours)" :delay
                  "lmst" :taken-martian
                  "name" :id
                  "released (UTC)" :released
                  "sol" :sol
                  "taken (UTC)" :taken-utc
                  "thumbnail" nil
                  "type" :type
                  "width" :w})

(def cam-abbrev->name {"ML" "MastCam Left"})

(def cell->int (comp #(Integer/parseInt %) html/text))

(def utc-format (fmt-time/formatter "YYYY MMM dd HH:mm:ss"))
(def utc-parser (comp (% fmt-time/parse utc-format) lower-case))
(def cell->utc-date (comp utc-parser trim html/text))

(def marstime-format (fmt-time/formatter "hh:mm:ss a"))
(def marstime-parser (% fmt-time/parse marstime-format))
(def cell->mars-time (comp marstime-parser trim html/text))

(defmulti cell->data
  (fn [label _] label))

(defmethod cell->data :cam
  [_ cell]
  (html/text cell))

(defmethod cell->data :delay
  [_ cell]
  (cell->int cell))

(defmethod cell->data :h
  [_ cell]
  (cell->int cell))

(defmethod cell->data :id
  [_ cell]
  (html/text cell))

(defmethod cell->data :released
  [_ cell]
  (cell->utc-date cell))

(defmethod cell->data :sol
  [_ cell]
  (cell->int cell))

(defmethod cell->data :taken-martian
  [_ cell]
  (cell->mars-time cell))

(defmethod cell->data :taken-utc
  [_ cell]
  (cell->utc-date cell))


(let [tables (html/select first-page [:table])
      img-table (last tables)
      [labels & rows] (html/select img-table [:tr])
      labels (->> (html/select labels [:th])
               (map (comp trim html/text))
               (replace label->attr)
               (keep identity))
      first-row (first rows)
      row-cells #(html/select % [:td])]
  (row-cells first-row))

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!"))
