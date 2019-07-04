(ns clj-uuid.node-test
  (:require [clojure.test   :refer :all]
            [clj-uuid.node  :refer :all]))


(deftest check-node-id
  (testing "existance and type of node id...")
  (is (= (node-id) (node-id)))
  (is (coll? (node-id)))
  (is (= 6 (count (node-id))))
  (is (every? number? (node-id)))
  (is (= 1 (bit-and 0x01 @+node-id+)))
  (is (instance? Long @+node-id+)))
