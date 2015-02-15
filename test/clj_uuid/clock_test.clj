(ns clj-uuid.clock-test
  (:require [clojure.test   :refer :all]
            [clojure.set]
            [clj-uuid.clock :refer :all]))


(deftest check-monotonic-time
  (doseq [concur (range 5 9)]
    (let [extent    100000
          agents    (map agent (repeat concur nil))
          working   (map #(send-off %
                            (fn [state]
                              (repeatedly extent monotonic-time)))
                      agents)
          _         (apply await working)
          answers   (map deref working)]
      (testing "single-thread timestamp uniqueness..."
        (is (=
              (* concur extent)
              (apply + (map (comp count set) answers)))))
      (testing "concurrent timestamp uniqueness..."
        (is (=
              (* concur extent)
              (count (apply clojure.set/union (map set answers))))))
      (testing "concurrent monotonic increasing..."
        (is (every? identity
              (map #(apply < %) answers)))))))




















