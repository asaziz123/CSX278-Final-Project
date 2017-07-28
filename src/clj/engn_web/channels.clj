(ns engn-web.channels
  (:gen-class))

(def channels (atom {}))

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

(defn channel-get!
  "Returns the messages in the channel. The default limit is the last
   10 messages, unless the :limit key is set to a different value."
  [channel & {:keys [limit] :or {limit 10}}]
 (take limit (or (get @channels channel) '())))

(defn channel-list "Returns a list of the available channels" [] (keys @channels))
