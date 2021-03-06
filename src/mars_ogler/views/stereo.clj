(ns mars-ogler.views.stereo
  (:require [clj-time.core :as time]
            [clj-time.coerce :as cvt-time]
            [mars-ogler.cams :as cams]
            [mars-ogler.images :as images]
            [mars-ogler.times :as times])
  (:use [hiccup core page]
        [mars-ogler.image-utils :only [image->url]]))

(def mode->name
  {:stereo-cross "Stereo (Cross-Eyed)"
   :stereo-wall "Stereo (Wall-Eyed)"
   :wiggle "Wiggle"
   :anaglyph "Anaglyph (Red-Cyan)"})

(def modes [:anaglyph :stereo-cross :stereo-wall])

(defn stereo
  [{:keys [l_id r_id stereo-mode]}]
  (let [l-img (get-in @images/indices [:id (name l_id)])
        r-img (get-in @images/indices [:id (name r_id)])
        iw (:w l-img)
        ih (:h l-img)
        [l-path r-path] (for [img [l-img r-img]]
                          (-> img image->url java.net.URL. .getPath))]
    (html5
      [:head
       [:title "Stereo Pair Viewer | The Mars Ogler"]
       (include-css "/css/stereo.css")
       (include-css "http://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400")
       (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js")
       (include-js "/js/modernizr.custom.js")
       (include-js "/js/stereo.js")
       ]
      [:body
       [:div#container
        [:div#left
         [:img#left-img {:src l-path, :width iw, :height ih
                         :nominal-width iw, :nominal-height ih}]]
        [:div#right.hidden
         [:img#right-img {:src r-path, :width iw, :height ih}]]
        [:div#anaglyph.hidden]]
       [:div#ui
        [:span#mode-label "Viewing Mode: "]
        [:input#preferred-mode {:type "hidden" :value stereo-mode}]
        [:select#mode-select {:disabled true}
         (for [mode modes]
           [:option {:value mode, :selected (= mode :mono)}
            (mode->name mode)])]
        [:span.anaglyph-ui.hidden
         [:span#anaglyph-slider-label "Alignment: "]
         [:input#anaglyph-slider {:type "range",
                                  :min 0, :max 150, :step 1, :value 40}]]
        [:span#no-js "Sorry, the stereo pair viewer requires Javascript"]]
       [:div#anaglyph-instructions.anaglyph-ui.hidden
        [:h3 "A Helpful Note:"]
        [:p "The alignment defaults to a value that usually works well for
            panoramic shots, but doesn't work for every pair.
            If you're having trouble seeing the 3D, try moving the alignment
            slider with your glasses off until you see a particular feature
            (such as a rock) line up between the red and cyan images.
            If you need more room to align the images, make your browser window
            wider and refresh."]]
       [:div#footer
        [:div#footer-left
         "A component of the " [:a {:href "/"} "Mars Ogler"] ", built by Dan "
         "Lidral-Porter"]]])))
