(ns engn-web.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljs-time.core :as time]
              [cljs-time.format :as time-format]
              [cljs-time.coerce :as time-coerce]
              [reagent-material-ui.core :as ui]
              [ajax.core :refer [GET POST]]))


;; Logic

(defn log [& msg]
  (.log js/console (apply str msg)))

(defn error-handler [{:keys [status status-text]}]
  (log (str "something bad happened: " status " " status-text)))

(defonce channels (atom []))
(defonce msgs (atom []))
(defonce msg-entry (atom ""))
(defonce current-channel (atom ""))
(defonce scroll-msgs (atom false))

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
        :handler (fn [r] (println r)(reset! msgs r))}))

(defn open-channel [channel]
   (reset! current-channel channel)
   (messages-load channel))

(defn push [msgs msg]
  (conj (seq msgs) msg))

(defn messages-add! [channel msg]
  (POST (str "/channel/" channel)
       {:params {:msg msg}
        :response-format :json
        :format :json
        :keywords? true
        :error-handler error-handler
        :handler (fn [r] (log "msg posted to server"))})
  (swap! msgs push {:msg msg
                    :user user
                    :time (time-coerce/to-long (time/now))})
  (.setTimeout js/window #(.scrollTo js/window 0 (+ 0 (.-scrollHeight (.-body js/document)))) 250))

(defn add-msg! []
  (messages-add! @current-channel @msg-entry)
  (reset! msg-entry ""))


;; -------------------------
;; Views

(defn message [m]
  (let [name (:nickname (:user m))
        text (:msg m)
        formatter (time-format/formatter "MM/dd/yyyy hh:mm:ss")
        formatted-time (time-format/unparse formatter (time-coerce/from-long (:time m)))]
      [ui/Card
       [ui/CardHeader {:title name
                       :subtitle formatted-time
                       :avatar "https://s.gravatar.com/avatar/a9edda10d0e6fb75561f057d167a9077?s=480&r=pg&d=https%3A%2F%2Fcdn.auth0.com%2Favatars%2Fju.png";"http://placehold.it/50/55C1E7/fff&text=U"
                       :actAsExpander true
                       :showExpandableButton false}]
       [ui/CardText text]]))

(defn messages [ms]
    (for [msg ms]
      ^{:key msg} [message msg]))

(defn channel [c]
  [ui/ListItem {:primaryText (str "#" c) :onTouchTap #(open-channel c)}])

(defn channel-list []
  [ui/List
    (for [c @channels]
         [channel c])])

(defn icon [nme] [ui/FontIcon {:className "material-icons"} nme])
(defn color [nme] (aget ui/colors nme))

;; create a new theme based on the dark theme from Material UI
; (defonce theme-defaults {:muiTheme (ui/getMuiTheme
;                                     (-> ui/darkBaseTheme
;                                         (js->clj :keywordize-keys true)
;                                         (update :palette merge {:primary1Color (color "amber500")
;                                                                 :primary2Color (color "amber700")})
;                                         clj->js))})

(defn simple-nav []
  (let [is-open? (atom false)
        close #(reset! is-open? false)]
    (fn []
      [:div#header
       [ui/AppBar {:title "yipgo" :onLeftIconButtonTouchTap #(reset! is-open? true)}]
       [ui/Drawer {:open @is-open? :docked true}
        [ui/List
         [ui/ListItem {:on-click (fn []
                                   (accountant/navigate! "/")
                                   (close))}
          "Channels"]
         [ui/Divider]
         [channel-list]]]])))

(defn main-page []
  [ui/MuiThemeProvider ;theme-defaults
   [:div
    [simple-nav]
    [:div {:style {:padding "10px 10px 90px 10px"}}
     [:h2 (str "#" @current-channel)]
     (messages (reverse @msgs))]
    [:footer
      [ui/Toolbar
        [ui/ToolbarGroup
          [:div {:style {:width "400px"}}]
          [ui/ToolbarSeparator]
          [ui/TextField
               {:style {:width "800px" :margin-left "20px"}
                :floatingLabelText "What would you like to say..."
                :floatingLabelFixed false
                :onChange #(reset! msg-entry (-> % .-target .-value))
                :value @msg-entry}]
          [ui/RaisedButton {:label "Send" :primary true :onTouchTap add-msg!}]]]]]])


;; -------------------------
;; Routes

(def page (atom #'main-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'main-page))


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
