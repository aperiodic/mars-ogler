(ns mars-ogler.scrape
  (:require [clojure.string :as str]
            [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [clj-time.format :as fmt-time]
            [mars-ogler.cams :as cams]
            [net.cgrand.enlive-html :as html]))
  (def % partial)

;;
;; Attributes
;;

(def image-attributes
  #{:w :h :id :url
    :cam :delay :released :sol :taken-martian :taken-utc :thumbnail :type})

(defn classify-size
  [{:keys [w h type] :as image}]
  (let [long-dim (max w h)
        size (cond
               (or (= type "I") (= type "T")) :thumbnail
               (>= long-dim 512) :large
               (>= long-dim 256) :medium
               :otherwise :small)]
    (assoc image :size size)))

;;
;; Scrapin'
;;

(def cell->int (comp #(Integer/parseInt %) html/text))

(def rfc-printer (% fmt-time/unparse (fmt-time/formatters :rfc822)))

(def utc-format (fmt-time/formatter "YYYY MMM dd HH:mm:ss"))
(def utc-parser (comp (% fmt-time/parse utc-format) str/lower-case))
(defn cell->utc-date-str
  [cell]
  (-> cell
    html/text str/trim utc-parser rfc-printer))

(def marstime-format (fmt-time/formatter "hh:mm:ss a"))
(def marstime-parser (% fmt-time/parse marstime-format))
(defn cell->marstime-str
  [cell]
  (-> cell
    html/text str/trim (str/replace #"\." "") marstime-parser rfc-printer))

(defn label->cell-type
  [label]
  (-> label (str/split #" ") first keyword))

(defmulti cell->map
  (fn [label _] (label->cell-type label)))

(defmethod cell->map :camera
  [_ cell]
  (let [abbrev (-> (html/text cell) str/trim)]
    {:cam (cams/cams-by-abbrev abbrev)
     :cam-name (cams/cam-names-by-abbrev abbrev)}))

(defmethod cell->map :delay
  [_ cell]
  {:delay (cell->int cell)})

(defmethod cell->map :height
  [_ cell]
  {:h (cell->int cell)})

(defmethod cell->map :lag
  [_ _]
  {})

(defmethod cell->map :lmst
  [_ cell]
  {:taken-marstime (cell->marstime-str cell)})

(defmethod cell->map :name
  [_ cell]
  {:id (-> (html/text cell) str/trim)
   :url (-> (html/select cell [:a])
          first
          (get-in [:attrs :href]))})

(defmethod cell->map :released
  [_ cell]
  {:released (cell->utc-date-str cell)})

(defmethod cell->map :sol
  [_ cell]
  {:sol (cell->int cell)})

(defmethod cell->map :taken
  [_ cell]
  {:taken-utc (cell->utc-date-str cell)})

(defmethod cell->map :thumbnail
  [_ cell]
  {:thumbnail-url (-> (html/select cell [:img])
                    first
                    (get-in [:attrs :src]))})

(defmethod cell->map :type
  [_ cell]
  {:type (-> (html/text cell) str/trim)})

(defmethod cell->map :width
  [_ cell]
  {:w (cell->int cell)})

(defn row->url
  [row]
  (let [img-link (-> (html/select row [:a]) first)]
    (get-in img-link [:attrs :href])))

(defn row->map
  [labels row]
  (let [cell-data (->> (html/select row [:td])
                    (map cell->map labels)
                    (apply merge))
        row-data {:url (row->url row)}]
    (merge cell-data row-data)))

(defn body->images
  [body-node]
  (let [img-table (-> (html/select body-node [:table]) last)
        [label-row & img-rows] (html/select img-table [:tr])
        labels (->> (html/select label-row [:th])
                 (map (comp str/trim html/text)))]
    (->> img-rows
      (map (% row->map labels))
      (map classify-size))))

;;
;; Fetchin'
;;

(defn page-url
  ([] (page-url 1))
  ([i] (str "http://curiositymsl.com/?page=" i "&limit=100")))

(defn fetch-page
  [i]
  (html/html-resource (java.net.URL. (page-url i))))

(defn fetch-images-since
  [last-id]
  (let [last-img? (fn [img] (= (:id img) last-id))]
    (println "fetching page 1")
    (loop [i 1, imgs ()]
      (let [page-imgs (body->images (fetch-page i))
            last-page? (some last-img? page-imgs)
            imgs' (concat imgs (take-while (complement last-img?) page-imgs))]
        (if (or last-page? (empty? page-imgs))
          imgs'
          (do
            (Thread/sleep 500) ; ratelimit ourselves
            (println "fetching page" (inc i))
            (recur (inc i) imgs')))))))

;;
;; State & Main
;;

(def $images-file "cache/images")

(defn cached-images
  "Read the images cached in the `$images-file`."
  []
  (try (-> (slurp $images-file) read-string)
    (catch java.io.FileNotFoundException _ )))

(defn dump-all-images!
  [images]
  (spit $images-file (prn-str images)))

(defn print-summary
  [new-images all-images]
  (println
    "fetched" (count new-images) "new images, have" (count all-images) "in"
    "total"))

(defn fetch-new-images!
  []
  (let [old-imgs (cached-images)
        last-id (-> old-imgs first :id)
        new-imgs (seq (fetch-images-since last-id))
        all-imgs (concat new-imgs old-imgs)
        new-id (-> all-imgs first :id)]
    (dump-all-images! all-imgs)
    (print-summary new-imgs all-imgs)))

(defn -main
  "Fetch new images and add them to the list of known images"
  [& _]
  (fetch-new-images!))
