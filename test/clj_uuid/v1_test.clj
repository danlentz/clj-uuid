(ns clj-uuid.v1-test
  "Time based UUIDs tests"
  (:require [clj-uuid.clock :as clock]
            [clj-uuid.core  :as uuid]
            [clojure.set    :as set]
            [clojure.test   :refer :all]))

(deftest check-v1-single-threaded
  (let [iterations 1000000
        groups     10]
    (testing "single-thread v1 uuid uniqueness..."
      (dotimes [_ groups]
        (let [result (repeatedly iterations uuid/v1)]
          (is (= (count result) (count (set result)))))))))

(deftest check-v1-concurrency
  (doseq [concur (range 2 9)]
    (let [extent    1000000
          agents    (map agent (repeat concur nil))
          working   (map #(send-off %
                            (fn [state]
                              (repeatedly extent uuid/v1)))
                      agents)
          _         (apply await working)
          answers   (map deref working)]
      (testing (str "concurrent v1 uuid uniqueness (" concur " threads)...")
        (is (= (* concur extent)
               (count (apply set/union (map set answers))))))
      (testing (str "concurrent v1 monotonic increasing (" concur " threads)...")
        (is (every? identity
              (map #(apply < (map uuid/get-timestamp %)) answers)))))))

(deftest check-get-timestamp
  (let [time (clock/monotonic-time)]
    (with-redefs [clock/monotonic-time (constantly time)]
      (is (= time (uuid/get-timestamp (uuid/v1)))
          "Timestamp should be retrievable from v1 UUID"))))
