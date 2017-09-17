(ns engn-web.local-messaging)

;; ==========================================================================
;; Functions to add / list messages and channels
;; ==========================================================================

(defn messages-get [msgs channel]
  (get msgs channel))

(defn messages-add [msgs channel msg]
  (let [chn-msgs     (messages-get msgs channel)
        chn-new-msgs (conj (seq chn-msgs) msg)]
     (assoc msgs channel chn-new-msgs)))

(defn channels-list [msgs]
  (keys msgs))

(defn channels-add [msgs channel]
  (if (nil? (get msgs channel))
    (assoc msgs channel [])
    msgs))
