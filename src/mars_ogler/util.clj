(ns mars-ogler.util
  (:require [cheshire
             [core :as json]
             [generate :refer [add-encoder encode-number]]]
            [clj-time.format :refer [formatters unparse]]
            [clj-time.coerce :as timec]
            [clojure.string :as str]))

(defn to-camel-case [k]
  (let [s (if (keyword? k) (name k) k)]
    (str/replace s #"\-[A-z]" (fn [[dash letter]]
                            (.toUpperCase (str letter))))))

(defn generate-json [x]
  (json/generate-string x {:key-fn to-camel-case}))

(add-encoder java.util.Date
             (fn [date json-generator]
               (encode-number (timec/to-long date)
                              json-generator)))

(add-encoder org.joda.time.ReadableDateTime
             (fn [date json-generator]
               (encode-number (timec/to-long date)
                              json-generator)))
