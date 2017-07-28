(ns engn-web.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [engn-web.middleware :refer [wrap-middleware]]
            [ring.middleware.json :as json]
            [config.core :refer [env]]
            [engn-web.channels :as channels]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(def json-header {"Content-Type" "application/json"})

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))
   (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")
   (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css")
   (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js")])

(defn json [data]
  {:status 200 :headers json-header :body data})

(defn some-data [] (json {:key :abcd :foo "adsf2"}))

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))

(defn cards-page []
  (html5
    (head)
    [:body
     mount-target
     (include-js "/js/app_devcards.js")]))

(defn channel-get [id]
  (json (channels/channel-get! id)))

(defn channel-add! [id msg]
  (json (channels/channel-add! id msg)))

(defn channel-list [] (json (channels/channel-list)))

(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/channel" [] (channel-list))
  (GET "/channel/:id" [id] (channel-get id))
  (POST "/channel/:id" [id msg] (channel-add! id msg))
  (GET "/key" [] (some-data))
  (GET "/about" [] (loading-page))
  (GET "/cards" [] (cards-page))
  (resources "/")
  (not-found "Not Found"))

(def app (->  (wrap-middleware #'routes)
              (json/wrap-json-params)
              (json/wrap-json-response)))
