(ns engn-web.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [ajax.core :refer [GET POST]]))

;; -------------------------
;; Views

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(defonce channels (atom []))
(defonce msgs (atom []))

(GET "/channel" {:response-format :json
                 :keywords? true
                 :error-handler error-handler
                 :handler (fn [r] (reset! channels r)(println "swap done: " @channels))})

(defn messages-load [channel]
  (GET (str "/channel/" channel)
       {:response-format :json
        :keywords? true
        :error-handler error-handler
        :handler (fn [r] (reset! msgs r))}))

(defn message [m]
  [:li {:class "left clearfix"}
       [:span {:class "chat-img pull-left"}
              [:img {:src "http://placehold.it/50/55C1E7/fff&text=U" :alt "User Avatar" :class "img-circle"}]]
       [:div {:class "chat-body clearfix"}
             [:div {:class "header"}
                   [:strong {:class "primary-font"}
                            "Jack Sparrow"]
                   [:small {:class "pull-right text-muted"}
                           [:span {:class "glyphicon glyphicon-time"}
                                  "12 mins ago"]]]
             [:p (str m)]]])


(defn messages [ms]
  [:div {:class "container"}
      [:div {:class "row"}
          [:div {:class "col-md-14"}
              [:div {:class "panel-collapse" :id "collapseOne"}
                  [:div {:class "panel-body"}
                      [:ul {:class "chat"}
                        (for [msg ms]
                          [message msg])]]]]]])

(defn channel [c]
       [:li {:class "sidebar-brand"}
            [:a {:href "#" :on-click #(messages-load c)}
                (str c)]])

(defn channel-list []
       [:ul {:class "sidebar-nav"}
            (for [c @channels]
                 [channel c])])

(defn home-page []
  [:div#wrapper {:class "toggled"}
                [:div#sidebar-wrapper
                  [channel-list]]
                [:div#page-content-wrapper
                  [messages @msgs]]])
   ;[:div [:a {:href "/poo"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About engn-web"]
   [:div [:a {:href "/"} "go to the home page"]]])

;; -------------------------
;; Routes

(def page (atom #'home-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'home-page))

(secretary/defroute "/about2" []
  (reset! page #'about-page))

(secretary/defroute "/poo" []
  (reset! page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
