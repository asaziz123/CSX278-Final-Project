(ns engn-web.channels-test
  (:require [clojure.test :refer :all]
            [engn-web.local-messaging :refer :all]))

(deftest test-messages-add
  (testing "Correct addition of messages to queues"
    (is (= {"a" [nil]} (messages-add {} "a" nil)))
    (is (= {"a" ["b" 1 2]} (messages-add {"a" '(1 2)} "a" "b")))
    (is (= {"a" ["b" 1 2] :b '("c" :d 2)} (messages-add {"a" '(1 2) :b '("c" :d 2)} "a" "b")))
    (is (= {"a" ["b" 1 2] :b [2 :d "c"]}
           (-> {"a" '(1 2) :b '("c")}
               (messages-add "a" "b")
               (messages-add :b :d)
               (messages-add :b 2))))
    (is (= {"a" ["b"]} (messages-add {} "a" "b")))))

(deftest test-channel-get
   (testing "Correct retrieval of messages from a queue"
     (let [msgs {:a [3 2 1]
                 :b [10]
                 :c nil
                 :d (repeatedly 100 #(rand-int 10))}]
      (is (= [3 2 1] (messages-get msgs :a)))
      (is (= [10] (messages-get msgs :b)))
      (is (= nil (messages-get msgs :c)))
      (is (= 100 (count (messages-get msgs :d)))))))
