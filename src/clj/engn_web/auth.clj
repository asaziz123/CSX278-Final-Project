(ns engn-web.auth
  (:require [engn-web.middleware :refer [wrap-middleware]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ring.middleware.json :as json]
            [config.core :refer [env]]
            [auth0-ring.handlers :as auth0]
            [auth0-ring.middleware :refer [wrap-token-verification]]))

(def config (read-string (slurp (io/resource "config.edn"))))

(defn login-prop [props key]
  (let [val (get props key)
         id (name key)]
   (if-not (nil? val) (str id ":" (if (string? val) (str "\"" val "\"") (str val)) ""))))

(defn login-props [props names]
  (str/join "," (filter #(not (nil? %)) (map #(login-prop props %) names))))

(def login
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
  ;; database, session etc.
  ;;
  ;; Refer to the Auth0User API:
  ;; https://github.com/auth0/auth0-java-mvc-common/blob/master/src/main/java/com/auth0/Auth0User.java
  (println (.getUserId auth0-user)))

(defn auth-handler [handler]
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

(defn get-user [request]
  (println request)
  (let [user (clojure.walk/keywordize-keys (into {} (:user request)))]
    user))

(defn wrap-auth [handler]
      (-> handler
          auth-handler
          (wrap-token-verification config)))
