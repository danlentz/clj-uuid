(ns clj-uuid.clock-test
  (:require
    [clojure.test   :refer :all]
    [clj-uuid.clock :refer :all]))


(deftest check-node-id
  (is (= (make-node-id) (make-node-id) +node-id+)))

(deftest check-nanosec-monotonic
  (is (apply <= (repeat 100000 (get-internal-real-time)))))

(deftest check-millisec-monotonic
  (is (apply <= (repeat 10000   (get-universal-time)))))

(deftest check-cooked-timestamp-monotonic
  (is (apply <= (repeat 100000 (make-timestamp)))))

