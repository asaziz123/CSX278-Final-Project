(ns engn-web.channels-test
  (:require [clojure.test :refer :all]
            [engn-web.channels :refer :all]))

(deftest test-add-msg
  (testing "Correct addition of messages to queues"
    (is (= {"a" [nil]} (add-msg {} "a" nil)))
    (is (= {"a" ["b" 1 2]} (add-msg {"a" '(1 2)} "a" "b")))
    (is (= {"a" ["b" 1 2] :b '("c" :d 2)} (add-msg {"a" '(1 2) :b '("c" :d 2)} "a" "b")))
    (is (= {"a" ["b" 1 2] :b [2 :d "c"]}
           (-> {"a" '(1 2) :b '("c")}
               (add-msg "a" "b")
               (add-msg :b :d)
               (add-msg :b 2))))
    (is (= {"a" ["b"]} (add-msg {} "a" "b")))))

(deftest test-channel-add
   (testing "Correct addition of messages to the internal atom containing channel queues"
     (with-redefs [channels (atom {})]
      (is (= {"a" [nil]} (channel-add! "a" nil)))
      (is (= {"a" ["b" nil]} (channel-add! "a" "b")))
      (is (= {"a" ["b" nil] :b ["c"]} (channel-add! :b "c")))
      (is (= {"a" ["b" nil] :b [:d "c"]} (channel-add! :b :d)))
      (is (= {"a" ["b" nil] :a [1] :b [:d "c"]} (channel-add! :a 1)))
      (is (= {"a" ["b" nil] :a [1] :b [:d "c"] :c [1]} (channel-add! :c 1))))))

(deftest test-channel-get
   (testing "Correct retrieval of messages from a queue"
     (with-redefs [channels (atom {:a [3 2 1]
                                   :b [10]
                                   :c nil
                                   :d (repeatedly 100 #(rand-int 10))})]
      (is (= [3 2 1] (channel-get! :a)))
      (is (= [10] (channel-get! :b)))
      (is (= [] (channel-get! :c)))
      (is (= [3 2] (channel-get! :a :limit 2)))
      (is (= [] (channel-get! :a :limit 0)))
      (is (= 10 (count (channel-get! :d))))
      (is (= 50 (count (channel-get! :d :limit 50))))
      (is (= 100 (count (channel-get! :d :limit 101))))
      (is (= 100 (count (channel-get! :d :limit 100)))))))
