(ns mars-ogler.views.stereo
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [mars-ogler.cams :as cams]
            [mars-ogler.images :as images]
            [mars-ogler.times :as times])
  (:use [hiccup core page]))

(def mode->name
  {:stereo-cross "Stereo (Cross-Eyed)"
   :stereo-wall "Stereo (Wall-Eyed)"
   :wiggle "Wiggle"
   :anaglyph "Anaglyph (Red-Cyan)"})

(def modes [:stereo-cross :stereo-wall :wiggle :anaglyph])

(defn stereo
  [{:keys [l_id r_id]}]
  (let [l-img (get-in @images/indices [:id (name l_id)])
        r-img (get-in @images/indices [:id (name r_id)])
        [l-path r-path] (for [img [l-img r-img]]
                          (-> img :url java.net.URL. .getPath))]
    (html5
      [:head
       [:title "Stereo Pair Viewer | The Mars Ogler"]
       (include-css "/css/stereo.css")
       (include-css "http://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400")
       (include-js "//ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js")
       (include-js "/js/stereo.js")]
      [:body
       [:div#container
        [:div#left
         [:img#left-img {:src l-path
                         :width (:w l-img), :height (:h l-img)}]]
        [:div#right.hidden
         [:img#right-img {:src r-path
                          :width (:w r-img), :height (:h r-img)}]]]
       [:div#ui
        [:span#mode-label "Viewing Mode: "]
        [:select#mode-select {:disabled true}
         (for [mode modes]
           [:option {:value mode, :selected (= mode :mono)}
            (mode->name mode)])]
         [:span#no-js "Sorry, the stereo pair viewer requires Javascript"]]
       [:div#footer
        [:div#footer-left
         "A component of the " [:a {:href "/"} "Mars Ogler"] ", built by Dan "
         "Lidral-Porter"]]])))
