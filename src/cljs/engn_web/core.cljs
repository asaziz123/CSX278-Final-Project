(ns engn-web.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljs-time.core :as time]
              [cljs-time.format :as time-format]
              [cljs-time.coerce :as time-coerce]
              [ajax.core :refer [GET POST]]))

;; -------------------------
;; Views

(defn log [msg]
  (.log js/console msg))

(defn error-handler [{:keys [status status-text]}]
  (log (str "something bad happened: " status " " status-text)))

(defonce channels (atom []))
(defonce msgs (atom []))
(defonce msg-entry (atom ""))
(defonce current-channel (atom ""))

(defonce user (js->clj js/user :keywordize-keys true))

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

(defn open-channel [channel]
   (reset! current-channel channel)
   (messages-load channel))

(defn push [msgs msg]
  (conj (seq msgs) msg))

(defn messages-add! [channel msg]
  (log "add msg")
  (POST (str "/channel/" channel)
       {:params {:msg msg}
        :response-format :json
        :format :json
        :keywords? true
        :error-handler error-handler
        :handler (fn [r] (log "msg posted to server"))})
  (swap! msgs push {:msg msg
                    :user user
                    :time (time-coerce/to-long (time/now))}))

(defn add-msg! []
  (messages-add! @current-channel @msg-entry)
  (reset! msg-entry ""))


(defn message [m]
  (let [formatter (time-format/formatter "MM/dd/yyyy hh:mm:ss")
        formatted-time (time-format/unparse formatter (time-coerce/from-long (:time m)))]
   [:li {:class "left clearfix"}
        [:span {:class "chat-img pull-left"}
               [:img {:src "http://placehold.it/50/55C1E7/fff&text=U" :alt "User Avatar" :class "img-circle"}]]
        [:div {:class "chat-body clearfix"}
              [:div {:class "header"}
                    [:strong {:class "primary-font"}
                             (str (:nickname (:user m)))]
                    [:small {:class "pull-right text-muted"}
                            [:span {:class "glyphicon glyphicon-time"}
                                   (str formatted-time)]]]
              [:p (str (:msg m))]]]))


(defn messages [ms]
  [:div {:class "container"}
      [:div {:class "row"}
          [:div {:class "col-md-14"}
                [:ul {:class "chat"}
                  (for [msg ms]
                    [message msg])]]]])

(defn channel [c]
       [:li
            [:a {:href "#" :on-click #(open-channel c)}
                (str "#" c)]])

(defn channel-list []
    [:div
       [:ul {:class "sidebar-nav"}
            [:li {:class "sidebar-brand"}
                 [:h2 "Channels"]]
            (for [c @channels]
                 [channel c])]])

(defn home-page []
  [:div
   [:div#wrapper {:class "toggled"}
                 [:div#sidebar-wrapper
                   [channel-list]]
                 [:div#page-content-wrapper
                   [messages (reverse @msgs)]]
                 [:label {:class "sr-only" :for "inlineFormInputGroup"}]
                 [:div {:class "input-group input-group-lg mb-2 mr-sm-2 mb-sm-0"}
                       [:div {:class "input-group-addon"
                              :on-click #(add-msg!)}
                             "+"]
                       [:input {:type "text"
                                :class "form-control form-control-lg"
                                :id "inlineFormInputGroup"
                                :value @msg-entry
                                :on-change #(reset! msg-entry (-> % .-target .-value))}]]]])


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
