(ns mars-ogler.routes
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [mars-ogler.views :as views])
  (:use [compojure.core :only [defroutes GET]]
        [hiccup.middleware :only [wrap-base-url]]))

(defroutes the-routes
  (GET "/" [& params :as req]
    (views/filter-pics params))
  (route/resources "/")
  (route/not-found "You have wandered into a maze of twisty passages, all alike"))

(def handler
  (-> (handler/site the-routes) wrap-base-url))
