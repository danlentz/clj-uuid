(ns clj-uuid.v1-test
  (:require [clojure.test   :refer :all]
            [clojure.set]
            [clj-uuid :refer [v1 get-timestamp]]))


(deftest check-v1-concurrency
  (doseq [concur (range 5 9)]
    (let [extent    100000
          agents    (map agent (repeat concur nil))
          working   (map #(send-off %
                            (fn [state]
                              (repeatedly extent v1)))
                      agents)
          _         (apply await working)
          answers   (map deref working)]
      (testing "single-thread v1 uuid uniqueness..."
        (is (=
              (* concur extent)
              (apply + (map (comp count set) answers)))))
      (testing "concurrent v1 uuid uniqueness..."
        (is (=
              (* concur extent)
              (count (apply clojure.set/union (map set answers))))))
      (testing "concurrent v1 monotonic increasing..."
        (is (every? identity
              (map #(apply < (map get-timestamp %)) answers)))))))
