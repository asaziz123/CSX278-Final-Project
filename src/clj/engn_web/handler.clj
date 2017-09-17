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


;; ==========================================================================
;; Utility functions for serving up a JSON REST API
;; ==========================================================================

(def json-header
  "Utility function to set the appropriate headers in an
   HTTP response to return JSON to the client"
  {"Content-Type" "application/json"})

(defn json
  "Utility function to return JSON to the client"
  [data]
  {:status 200 :headers json-header :body data})


;; ==========================================================================
;; The JSON REST API for sending messages, listing channels,
;; etc.
;; ==========================================================================

(defn channel-list
  "List the set of currently known channels"
  [] (json (channels/channel-list)))

(defn channel-get
  "Return the messages in a specific channel"
  [id]
  (json (channels/channel-get id)))

(defn msg-create
  "Utility function to create a message data
   structure"
  [msg userobj]
  (let [{:keys [name nickname]} userobj
        user {:name name :nickname nickname}
        time (System/currentTimeMillis)]
      {:msg msg :time time :user user}))

(defn channel-add!
   "Add a message to the specified channel"
   [channel msg-data user-obj]
   (let [msg (msg-create msg-data user-obj)]
    (json (channels/channel-add! channel msg))))


;; ==========================================================================
;; Functions to render the HTML for the single-page application
;; ==========================================================================

(def mount-target
  "This is the page that is displayed before figwheel
   compiles your application"
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler (this page may self-destruct)"]])

(defn include-user
  "This function inserts the user information directly into a <script> tag
   so that it can be accessed via Javascript in the 'user' variable"
  [user]
  (if-not (nil? user)
    [:script
     ; (str "var user = {name:\"" (:name user)
     ;      "\",nickname:\"" (:nickname user)
     ;      "\", picture:\"" (:picture user) "\"}")]))
     (str "var user = {name:\"Your Name\",
          nickname:\"You\",
          picture:\"https://s.gravatar.com/avatar/a9edda10d0e6fb75561f057d167a9077?s=480&r=pg&d=https%3A%2F%2Fcdn.auth0.com%2Favatars%2Fyu.png\"}")]))

(defn head [user]
  "Function to generate the <head> tag in the main HTML page that is
   returned to the browser"
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "https://fonts.googleapis.com/icon?family=Material+Icons")
   (include-css "https://fonts.googleapis.com/css?family=Roboto:300,400,500")
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))
   (include-user user)])

(defn main-page
   "Generates the main HTML page for the single-page application"
   [user]
   (html5
     (head user)
     [:body
       mount-target
       (include-js "/js/app.js")]))


;; ==========================================================================
;; Functions to setup the list of URI routes for the application
;; and setup the appropriate middleware wrappers
;; ==========================================================================

(defroutes routes
  (GET "/" request (main-page (auth/get-user request)))
  (GET "/channel" [] (channel-list))
  (GET "/channel/:id" [id] (channel-get id))
  (POST "/channel/:id" [id msg :as request] (channel-add! id msg (auth/get-user request)))
  (resources "/")
  (not-found "Not Found"))


(def app (->  (wrap-middleware #'routes)
              ;auth/wrap-auth
              (json/wrap-json-response)
              (json/wrap-json-params)
              wrap-params
              wrap-cookies))
