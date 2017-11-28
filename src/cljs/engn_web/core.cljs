(ns engn-web.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljs-time.core :as time]
              [cljs-time.format :as time-format]
              [cljs-time.coerce :as time-coerce]
              [reagent-material-ui.core :as ui]
              [ajax.core :refer [GET POST]]))


;; ==========================================================================
;; Utility functions
;; ==========================================================================

(defn log
  "Log a message to the Javascript console"
  [& msg]
  (.log js/console (apply str msg)))

(defn error-handler
  "Error handler for AJAX calls to print out errors to the console"
  [{:keys [status status-text]}]
  (log (str "something bad happened: " status " " status-text)))

(def el reagent/as-element)
(defn icon-span [nme] [ui/FontIcon {:className "material-icons"} nme])
(defn icon [nme] (el [:i.material-icons nme]))
(defn color [nme] (aget ui/colors nme))

;; ==========================================================================
;; App State
;; ==========================================================================

(defonce channels (atom []))
(defonce msgs (atom []))
(defonce msg-entry (atom ""))
(defonce chnl-entry (atom ""))
(defonce current-channel (atom "default"))
(defonce scroll-msgs (atom false))
(defonce nav-open? (atom false))
(defonce add-channel-dialog-open? (atom false))
(defonce user (js->clj js/user :keywordize-keys true))


;; ==========================================================================
;; Functions to send / receive messages and list channels
;; ==========================================================================

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

(defn add-channel! [channel]
  (if (not-any? #(= channel %) @channels)
    (do
      (POST (str "/channel/" channel)
          {:params {:msg "Conversation start"}
           :response-format :json
           :format :json
           :keywords? true
           :error-handler error-handler
           :handler (fn [r] (log "msg posted to server"))})
      (swap! channels conj channel))))


;; ==========================================================================
;; View components
;; ==========================================================================

(defn message [m]
  (let [name (:nickname (:user m))
        text (:msg m)
        class (if @nav-open? "message open" "message closed")
        formatter (time-format/formatter "MM/dd/yyyy hh:mm:ss")
        formatted-time (time-format/unparse formatter (time-coerce/from-long (:time m)))]
      [ui/Card {:class class}
       [ui/CardHeader {:title name
                       :subtitle formatted-time
                       :avatar "https://s.gravatar.com/avatar/a9edda10d0e6fb75561f057d167a9077?s=480&r=pg&d=https%3A%2F%2Fcdn.auth0.com%2Favatars%2Fju.png";"http://placehold.it/50/55C1E7/fff&text=U"
                       :actAsExpander true
                       :showExpandableButton false}]
       [ui/CardText text]]))

(defn messages [ms]
  [:div {:style {:margin-bottom "100px"}}
    (for [msg ms]
      ^{:key msg} [message msg])])

(defn channel [c]
  [ui/ListItem {:leftIcon (icon "label_outline")
                ;:rightIcon [icon "chat"]
                :primaryText (str c)
                :onTouchTap #(open-channel c)}])


(defn channel-list []
  [ui/List
   [ui/Subheader "Conversations"
    [ui/ListItem {:leftIcon (icon "add_circle_outline")
                  :primaryText "Add Channel"
                  :onTouchTap #(do (println @add-channel-dialog-open?)(reset! add-channel-dialog-open? true))}]
    (for [c @channels]
         ^{:key c} [channel c])]])

(defn channel-add-dialog []
  (let [hide-dialog #(reset! add-channel-dialog-open? false)]
    [ui/Dialog {:open @add-channel-dialog-open?
                :title "Create New Channel"
                :actions [ (el [ui/FlatButton {:label "Cancel" :primary false :onTouchTap hide-dialog}])
                           (el [ui/FlatButton {:label "OK" :primary true :onTouchTap #(do
                                                                                        (add-channel! @chnl-entry)
                                                                                        (hide-dialog))}])]
                :modal false}
      [ui/TextField
           {:style {:width "400px" :margin-left "20px"}
            :floatingLabelText "Channel name"
            :floatingLabelFixed false
            :onChange #(reset! chnl-entry (-> % .-target .-value))}]]))
         ; :value @msg-entry}]])


;; create a new theme based on the dark theme from Material UI
; (defonce theme-defaults {:muiTheme (ui/getMuiTheme
;                                     (-> ui/darkBaseTheme
;                                         (js->clj :keywordize-keys true)
;                                         (update :palette merge {:primary1Color (color "amber500")
;                                                                 :primary2Color (color "amber700")})
;                                         clj->js))})

(defn simple-nav []
  (let [close #(reset! nav-open? false)]
    (fn []
      [:div#header
       [ui/AppBar {:title "Grief Net" :onLeftIconButtonTouchTap #(reset! nav-open? true) :style {:backgroundColor "#81AA47"}}
        [ui/IconMenu {:iconButtonElement (el [ui/IconButton {:style {:color "#FFF"}} (icon "more_vert")])
                                         :targetOrigin {:horizontal "right" :vertical "top"}
                                         :anchorOrigin {:horizontal "right" :vertical "top"}}
          [ui/MenuItem {:primaryText "Sign out" :onTouchTap #(aset js/document "location" "/logout")}]]]
       [ui/Drawer {:open @nav-open?
                   :docked true
                   :style {:z-index 10001}
                   :containerStyle {:z-index 10001}}
        [ui/AppBar {:title "Grief Net" :onLeftIconButtonTouchTap #(reset! nav-open? false) :style {:backgroundColor "#81AA47"}}]

        [ui/List
         [ui/ListItem {:disabled true}
             [ui/Avatar {:size 150 :src (:picture user) :style {:margin-left "30px"}}]]
         [ui/ListItem {:leftIcon (icon "person_outline")
                       :primaryText (:nickname user)}]
         [ui/ListItem {:leftIcon (icon "person_outline")
                       :primaryText (:name user)}]]
        [ui/List ;{:style {:background-color "#F9FBE7" :height "100%"}}
         [ui/Divider]
         [channel-list]]]])))

(defn main-page []
  (let [class (if @nav-open? "message open" "message closed")]
   [ui/MuiThemeProvider ;theme-defaults
    [:div
     [channel-add-dialog]
     [simple-nav]
     [:div {:style {:padding "10px 10px 10px 10px"}}
      [ui/Card {:class class :style {:margin-top "60px"}}
        [ui/CardHeader {:title (str @current-channel) :titleStyle {:fontSize "200%"}}]]
      (messages (reverse @msgs))]
     [:footer {:class class}
       [ui/Toolbar {:style {:align "center"}}
         [ui/ToolbarGroup {:style {:width "70%"}}
           [:div {:style {:width "10px"}}]
           [ui/ToolbarSeparator]
           [ui/TextField
                {:style {:width "800px" :margin-left "20px"}
                 :floatingLabelText "What would you like to say..."
                 :floatingLabelFixed false
                 :onChange #(reset! msg-entry (-> % .-target .-value))
                 :value @msg-entry
                 :on-key-press (fn [e](if (= 13 (.-charCode e)) (add-msg!)))}]
           [ui/RaisedButton {:label "Send" :onTouchTap add-msg! :backgroundColor "#81AA47" :labelColor "#FFF"}]]]]]]))



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
