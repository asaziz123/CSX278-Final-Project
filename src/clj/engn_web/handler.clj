(ns engn-web.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [engn-web.middleware :refer [wrap-middleware]]
            [ring.middleware.json :as json]
            [config.core :refer [env]]
            [engn-web.channels :as channels]
            [engn-web.auth :as auth]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(def json-header {"Content-Type" "application/json"})

(defn head [user]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))
   (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")
   (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css")
   (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js")
   (if-not (nil? user)
     [:script
      (str "var user = {name:\"" (:name user) "\",nickname:\"" (:nickname user) "\"}")])])

(defn json [data]
  {:status 200 :headers json-header :body data})

(defn loading-page [user]
   (html5
     (head user)
     [:body
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

(defn msg-create [msg userobj]
 (let [{:keys [name nickname]} userobj
       user {:name name :nickname nickname}
       time (System/currentTimeMillis)]
      {:msg msg :time time :user user}))

(defn channel-add! [id msg-data user-obj]
   (let [msg (msg-create msg-data user-obj)]
    (json (channels/channel-add! id msg))))

(defn channel-list [] (json (channels/channel-list)))

(defroutes routes
  (GET "/" request (loading-page (auth/get-user request)))
  (GET "/channel" [] (channel-list))
  (GET "/channel/:id" [id] (channel-get id))
  (POST "/channel/:id" [id msg :as request] (channel-add! id msg (auth/get-user request)))
  (GET "/cards" [] (cards-page))
  (resources "/")
  (not-found "Not Found"))


(def app (->  (wrap-middleware #'routes)
              auth/wrap-auth
              (json/wrap-json-response)
              (json/wrap-json-params)
              wrap-params
              wrap-cookies))
