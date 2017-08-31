(ns engn-web.auth
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [auth0-ring.handlers :as auth0]
            [auth0-ring.middleware :refer [wrap-token-verification]]))

;; Load the configuration file with the Auth0 account info
(def config (read-string (slurp (io/resource "config.edn"))))

(defn login-prop
  "Helper function to insert Javascript properties into the
  Auth0 configuration object"
  [props key]
  (let [val (get props key)
         id (name key)]
   (if-not (nil? val) (str id ":" (if (string? val) (str "\"" val "\"") (str val)) ""))))

(defn login-props
  "Helper function to insert multiple Javascript properties into
   the Auth0 configuration object"
  [props names]
  (str/join "," (filter #(not (nil? %)) (map #(login-prop props %) names))))

(def login
   "Function to render the Auth0 login page"
   (auth0/wrap-login-handler
    (fn [req]
      (let [logo (or (:logo config))]
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (str "<!DOCTYPE html>
                    <html>
                      <head>
                        <title>Login</title>
                      </head>
                      <body>
                        <script src='https://cdn.auth0.com/js/lock/10.9.1/lock.min.js'></script>
                        <script>var lock = new Auth0Lock(
                    '" (:client-id config) "',
                    '" (:domain config) "', {
                      languageDictionary: {
                           " (login-props config [:title :emailInputPlaceholder]) "
                       },
                       theme: {
                           " (login-props config [:logo :primaryColor]) "
                         },
                      auth: {
                        params: {
                          scope: '" (:scope config) "',
                          state: 'nonce=" (:nonce req) "&returnUrl=" (get-in req [:query-params "returnUrl"]) "'
                        },
                        responseType: 'code',
                        redirectUrl: window.location.origin + '" (:callback-path config) "'
                      },
                      closeable: false
                    });

                    lock.show();</script>
                      </body>
                    </html>")}))))


(defn auth-callback [auth0-user]
  ;; Optional hook for when you need to sync user profile details into a local
  (println (.getUserId auth0-user)))

(defn auth-handler
  "Generic authenication middlware that handles displaying a login page with
   Auth0, login/logout paths, and token callbacks from Auth0."
  [handler]
  (fn [req]
   (let [callback-handler (auth0/create-callback-handler config)
         logout-callback-handler (auth0/create-logout-callback-handler config)
         logout-handler (auth0/create-logout-handler config)
         logged-in (not (nil? (:user req)))]
     (case (:uri req)
      "/login" (login req)
      "/auth/callback" (callback-handler req)
      "/auth/logout" (logout-callback-handler req)
      "/logout" (logout-handler req)
      "/favicon.ico" {:status 404}
      (if logged-in (handler req) (ring.util.response/redirect "/login"))))))

(defn get-user
  "Helper function to extract the current user information from Auth0 into a
   Clojure dictionary"
  [request]
  (let [user (clojure.walk/keywordize-keys (into {} (:user request)))]
    user))

(defn wrap-auth
  "Helper function to insert the authentication middleware for login/logout
   with Auth0"
  [handler]
  (-> handler
      auth-handler
      (wrap-token-verification config)))
