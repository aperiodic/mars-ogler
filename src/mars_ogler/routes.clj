(ns mars-ogler.routes
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [mars-ogler.views :as views])
  (:use [compojure.core :only [defroutes GET]]
        [ring.middleware cookies
                         keyword-params
                         params]))

(defroutes the-routes
  (GET "/" [& params]
    (views/pics params))
  (route/resources "/")
  (route/not-found "You have wandered into a maze of twisty passages, all alike"))

(def handler
  (-> the-routes
    wrap-keyword-params
    wrap-params
    wrap-cookies))
