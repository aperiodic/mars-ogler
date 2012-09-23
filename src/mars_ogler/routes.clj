(ns mars-ogler.routes
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [mars-ogler.views :as views])
  (:use [compojure.core :only [defroutes GET]]
        [ring.middleware cookies
                         keyword-params
                         params]))

(defroutes ogler-routes
  (GET "/" [& params]
    (views/index params))
  (route/resources "/")
  (route/not-found "You have wandered into a maze of twisty passages, all alike"))

(def ogler-handler
  (-> ogler-routes
    wrap-keyword-params
    wrap-params
    wrap-cookies))
