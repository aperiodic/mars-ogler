(ns mars-ogler.scrape
  (:require [clj-http.client :as http]
            [clj-time.core :as time]
            [clojure.string :as str]
            [mars-ogler.cams :as cams]
            [mars-ogler.images :as images]
            [mars-ogler.times :as times]
            [net.cgrand.enlive-html :as html]))
  (def % partial)

;;
;; Scrapin'
;;

(defn id->camera
  [id]
  (let [abbrev (re-find #"[A-Z]+" id)]
    {:cam (cams/abbrev->cam abbrev)
     :cam-name (cams/abbrev->name abbrev)}))

(defn id->type
  [id]
  (let [mast-mahli-mardi #"([A-Z])\d_"
        nav-chem-haz #"_([A-Z])"]
    (if-let [type (second (re-find mast-mahli-mardi id))]
      type
      (second (re-find nav-chem-haz id)))))

(defn div->times
  [div]
  (let [[marstime taken released] (-> div
                                    (html/select [:.rawtable]), first
                                    (html/select [:tr]), second
                                    (html/select [:td])
                                    (->> (map #(-> % :content second str/trim))))]
    [(-> marstime (str/replace #"\." "") times/marstime-parser times/rfc-printer)
     (-> taken times/utc-parser times/rfc-printer)
     (-> released times/utc-parser times/rfc-printer)
     (-> (time/now) times/rfc-printer)]))

(defn div->map
  [div]
  (let [rawtable (-> (html/select div [:.rawtable]) first)
        id (-> (html/select rawtable [:a]) first html/text str/trim)
        {:keys [cam cam-name]} (id->camera id)
        type (id->type id)
        sol (->> (html/text rawtable) (re-find #"sol (\d+)") second Integer.)
        [marstime taken-utc released acquired] (div->times div)
        thumbnail-url (-> (html/select div [:.thumbnail :a :img])
                        first :attrs :src)
        url (-> (html/select div [:.rawtable :a]) first :attrs :href)
        dims (->> (re-find #"(\d+)x(\d+)" (html/text div)) (drop 1))
        [w h] (for [dim dims] (Integer/parseInt dim))]
    {:w w, :h h, :id id, :cam cam, :cam-name cam-name :type type, :sol sol
     :taken-marstime marstime :taken-utc taken-utc, :released released
     :acquired acquired, :thumbnail-url thumbnail-url, :url url}))

(defn classify-size
  [{:keys [w h type] :as image}]
  (let [long-dim (max w h)
        size (cond
               (or (= type "I") (= type "T") (= type "Q")) :thumbnail
               (>= long-dim 1024) :large
               (>= long-dim 512) :medium
               :otherwise :small)]
    (assoc image :size size)))

(defn body->images
  [body-node]
  (let [image-divs (html/select body-node [:body :> :div])]
    (->> image-divs
      (map div->map)
      (map classify-size)
      (map images/parse-dates))))

;;
;; Fetching Stuff Over the Tubes
;;

(defn fetch-page
  [i]
  (let [page (:body (http/post
                      "http://curiositymsl.com/localphp/loadmore.php"
                      {:headers {"Cookie" "PHPSESSID=3v55ibi7c0l25836c2chenpmo1"
                                 "User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/537.13 (KHTML, like Gecko) Chrome/24.0.1290.1 Safari/537.13"
                                 "X-Requested-With" "XMLHttpRequest"}
                       :form-params {:q "", :z "UTC", :s "etreleased",
                                     :l "all" :o "desc", :n 100
                                     :start (* (dec i) 100)}}))
        tmp-file "/tmp/ogle-scrape"]
    (spit tmp-file page)
    (-> (html/html-resource (java.io.File. tmp-file)) first)))

(defn fetch-new-images
  [known-imgs]
  (let [seen? (fn [img] (some #(= (:id %) (:id img)) (take 100 known-imgs)))]
    (println "Checking for new photos...")
    (loop [i 1, imgs ()]
      (let [page-imgs (body->images (fetch-page i))
            last-page? (some seen? page-imgs)
            imgs' (concat imgs (take-while (complement seen?) page-imgs))]
        (if (or last-page? (empty? page-imgs))
          imgs'
          (do
            (Thread/sleep 500) ; ratelimit ourselves
            (println "Oh boy, there's a lot! Fetching page" (inc i))
            (recur (inc i) imgs')))))))

;;
;; Scrape Loop & Friends
;;

(defn print-summary
  [new-images all-images]
  (if (empty? new-images)
  (println "I didn't find any new photos, so we still have"
           (count all-images) "total")
  (println "I found" (count new-images) "new photos, so now we have"
             (count all-images) "total")))

(defn fetch-tick
  [old-imgs]
  (let [new-imgs (fetch-new-images old-imgs)
        all-imgs (concat new-imgs old-imgs)]
    {:all all-imgs, :new new-imgs, :old old-imgs}))

(defn scrape-loop!
  []
  (let [backup-imgs (atom (images/get-cached-images))
        scrape-period (* 15 60 1000)] ; 15 minutes
    (while true
      (try (loop [imgs @backup-imgs]
             (let [{:keys [new old all]} (fetch-tick imgs)]
               (print-summary new all)
               (when-not (empty? new)
                 (images/update! all)
                 (reset! backup-imgs all))
               (Thread/sleep scrape-period)
               (recur all)))
        (catch Throwable err
          (let [cause (.getCause err)
                err-msg (str (.getMessage err)
                             (and cause
                                  (str " caused by: ") (.getMessage cause)))]
            (println "Had some trouble in the scrape loop:" err-msg))
          (Thread/sleep scrape-period))))))
