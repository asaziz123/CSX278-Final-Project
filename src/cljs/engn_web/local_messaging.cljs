(ns engn-web.local-messaging
    (:require [cljs-time.core :as time]
              [cljs-time.format :as time-format]
              [cljs-time.coerce :as time-coerce]))


(defn log
  "Log a message to the Javascript console"
  [& msg]
  (.log js/console (apply str msg)))

;; ==========================================================================
;; Functions to send / receive messages and list channels
;; ==========================================================================

(defn messages-get [msgs channel]
  (get msgs channel))

(defn messages-add [msgs channel msg]
  (let [msg-data     {:msg msg
                      :user {:name "Your Name" :nickname "You"}
                      :time (time-coerce/to-long (time/now))}
        chn-msgs     (messages-get msgs channel)
        chn-new-msgs (conj (seq chn-msgs) msg-data)]
     (assoc msgs channel chn-new-msgs)))

(defn channels-list [msgs]
  (keys msgs))

(defn channels-add [msgs channel]
  (if (nil? (get msgs channel))
    (messages-add msgs channel "Channel started")
    msgs))
