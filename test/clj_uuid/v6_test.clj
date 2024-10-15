(ns clj-uuid.v6-test
  "Time based UUID tests"
  (:require [clj-uuid.clock :as clock]
            [clj-uuid.core  :as uuid]
            [clojure.set    :as set]
            [clojure.test   :refer :all]))

(deftest check-v6-single-threaded
  (let [iterations 1000000
        groups     10]
    (testing "single-thread v6 uuid uniqueness..."
      (dotimes [_ groups]
        (let [result (repeatedly iterations uuid/v6)]
          (is (= (count result) (count (set result)))))))))

(deftest check-v6-concurrency
  (doseq [concur (range 2 9)]
    (let [extent    1000000
          agents    (map agent (repeat concur nil))
          working   (map #(send-off %
                            (fn [state]
                              (repeatedly extent uuid/v6)))
                      agents)
          _         (apply await working)
          answers   (map deref working)]
      (testing (str "concurrent v6 uuid uniqueness (" concur " threads)...")
        (is (= (* concur extent)
               (count (apply set/union (map set answers))))))
      (testing (str "concurrent v6 monotonic increasing (" concur " threads)...")
        (is (every? identity (map (partial apply uuid/<) answers)))))))

(deftest check-get-timestamp
  (dotimes [_ 1000000]
    (let [time (clock/monotonic-time)]
      (with-redefs [clock/monotonic-time (constantly time)]
        (is (= time (uuid/get-timestamp (uuid/v6)))
            "Timestamp should be retrievable from v6 UUID")))))
