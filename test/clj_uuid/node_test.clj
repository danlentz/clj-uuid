(ns clj-uuid.node-test
  (:require [clj-uuid.node :as node]
            [clojure.test  :refer :all]))


(deftest check-node-id
  (testing "existance and type of node id...")
  (is (= (node/node-id) (node/node-id)))
  (is (coll? (node/node-id)))
  (is (= 6 (count (node/node-id))))
  (is (every? number? (node/node-id)))
  (is (= 1 (bit-and 0x01 @node/+node-id+)))
  (is (instance? Long @node/+node-id+)))
