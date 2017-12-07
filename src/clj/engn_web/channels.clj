(ns engn-web.channels
  (:gen-class))

;; We use a thread-safe atom to maintain the list of messages
;; that have been sent
(def channels (atom {"Welcome" [{:msg "Welcome to TalkingPlace! This community is here to help you cope with grief." :time (System/current/currentTimeMillis) :user {:name "Welcome" :nickname "Welcome"} :hide false}]
"Loss of Family Member" [{:msg "Hi. I recently lost my uncle to cancer. I'm looking for coping strategies to help me when I'm alone." :time (System/current/currentTimeMillis) :user {:name "Ben" :nickname "Ben"} :hide false}] "Loss of a Friend" [{:msg "Hi. One of my friends back home recently committed suicide and it's just been really hard for me to get through the week. I just need to hear from people who understand what I'm going through to offer encouragement." :time (System/current/currentTimeMillis) :user {:name "Austin" :nickname "Austin"} :hide true}]  "Divorce" [] "Financial Issues" []}))

(defn add-msg
  "Expects to receive a map of channels of the form:

   { :some-name (msg1, msg2, ...)
     :some-name2 (msg1, ...)}

  Finds the channel specified by q-name and adds a msg
  to it:

  (add-msg {:a '()} :a 1) => {:a '(1)}"
  [q-map q-name msg]
  (assoc q-map q-name (conj (get q-map q-name) msg)))

(defn channel-add!
  "Updates a global state of channels and the messages they
   contain by appending a message to the channel that is specified."
  [channel msg]
  (swap! channels add-msg channel msg))

(defn channel-get
  "Returns the messages in the channel. The default limit is the last
   500 messages, unless the :limit key is set to a different value."
  [channel & {:keys [limit] :or {limit 500}}]

(take limit (or
(filter #(< (- (System/current/currentTimeMillis) (:time %))  604800000)
(get @channels channel)
)
 '()))
)

(defn channel-list "Returns a list of the available channels" [] (keys @channels))
