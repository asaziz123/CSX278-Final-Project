(ns engn-web.core-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [reagent.core :as reagent :refer [atom]]
            [engn-web.core :as rc]))


(def isClient (not (nil? (try (.-document js/window)
                              (catch js/Object e nil)))))

(def rflush reagent/flush)

(defn add-test-div [name]
  (let [doc     js/document
        body    (.-body js/document)
        div     (.createElement doc "div")]
    (.appendChild body div)
    div))

(defn with-mounted-component [comp f]
  (when isClient
    (let [div (add-test-div "_testreagent")]
      (let [comp (reagent/render-component comp div #(f comp div))]
        (reagent/unmount-component-at-node div)
        (reagent/flush)
        (.removeChild (.-body js/document) div)))))


(defn found-in [re div]
  (let [res (.-innerHTML div)]
    (if (re-find re res)
      true
      (do (println "Not found: " res)
          false))))


(deftest test-anonymizing
  (let [user1 {:name "User1" :nickname "U1"}
        user2 {:name "User2" :nickname "U2"}]
    (is (= (rc/anonymize-user user1) (rc/anonymize-user user1)))
    (is (= (rc/anonymize-user user2) (rc/anonymize-user user2)))
    (is (not= (rc/anonymize-user user1) (rc/anonymize-user user2)))
    (is (not= (rc/anonymize-user user2) (rc/anonymize-user user1)))))


(deftest test-home
  (with-mounted-component (rc/home-page)
    (fn [c div]
      (is (found-in #"Welcome to" div)))))
