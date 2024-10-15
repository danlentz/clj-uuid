(ns clj-uuid.v7-test
  (:require [clj-uuid.clock :as clock]
            [clj-uuid.core  :as uuid]
            [clojure.set    :as set]
            [clojure.test   :refer :all]))

(deftest check-v7-single-threaded
  (let [iterations 1000000
        groups     10]
    (testing "single-thread v7 uuid uniqueness..."
      (dotimes [_ groups]
        (let [result (repeatedly iterations uuid/v7)]
          (is (= (count result) (count (set result)))))))))

(deftest check-v7-concurrency
  (doseq [concur    (range 2 9)]
    (let [extent    1000000
          agents    (map agent (repeat concur nil))
          working   (map #(send-off %
                            (fn [state]
                              (repeatedly extent uuid/v7)))
                      agents)
          _         (apply await working)
          answers   (map deref working)]
      (testing (str "concurrent v7 uuid uniqueness (" concur " threads)...")
        (is (= (* concur extent)
               (count (apply set/union (map set answers))))))

      (testing (str "concurrent v7 monotonic increasing (" concur " threads)...")
        (is (every? identity
                    (map (partial apply uuid/<) answers)))))))

(deftest check-get-timestamp
  (dotimes [_ 1000000]
    (let [time (first (clock/monotonic-unix-time-and-random-counter))]
      (with-redefs [clock/monotonic-unix-time-and-random-counter (constantly [time (rand-int 4095)])]
        (is (= time (uuid/get-timestamp (uuid/v7)))
            "Timestamp should be retrievable from v7 UUID")))))
