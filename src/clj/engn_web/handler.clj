(ns engn-web.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [config.core :refer [env]]
            [engn-web.middleware :refer [wrap-middleware]]
            [hiccup.page :refer [include-js include-css html5]]
            [ring.middleware.json :as json]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]))


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
  []
  [:script
   (str "var user = {name:\"Your Name\",
        nickname:\"You\",
        picture:\"https://s.gravatar.com/avatar/a9edda10d0e6fb75561f057d167a9077?s=480&r=pg&d=https%3A%2F%2Fcdn.auth0.com%2Favatars%2Fyu.png\"}")])

(defn head []
  "Function to generate the <head> tag in the main HTML page that is
   returned to the browser"
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "https://fonts.googleapis.com/icon?family=Material+Icons")
   (include-css "https://fonts.googleapis.com/css?family=Roboto:300,400,500")
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))
   (include-user)])

(defn main-page
   "Generates the main HTML page for the single-page application"
   []
   (html5
     (head)
     [:body
       mount-target
       (include-js "/js/app.js")]))


;; ==========================================================================
;; Functions to setup the list of URI routes for the application
;; and setup the appropriate middleware wrappers
;; ==========================================================================

(defroutes routes
  (GET "/" request (main-page))
  (resources "/")
  (not-found "Not Found"))


(def app (->  (wrap-middleware #'routes)
              (json/wrap-json-response)
              (json/wrap-json-params)
              wrap-params
              wrap-cookies))
