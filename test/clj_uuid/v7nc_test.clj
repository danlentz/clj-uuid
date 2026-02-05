(ns clj-uuid.v7nc-test
  (:require [clojure.test :refer :all]
            [clojure.set]
            [clj-uuid :as uuid :refer [v7nc]]))

(deftest check-v7nc-version
  (testing "v7nc produces version 7 UUIDs"
    (dotimes [_ 100]
      (is (= 7 (uuid/get-version (v7nc)))))))

(deftest check-v7nc-variant
  (testing "v7nc produces variant 2 (RFC 4122) UUIDs"
    (dotimes [_ 100]
      (is (= 2 (.variant (v7nc)))))))

(deftest check-v7nc-single-threaded
  (let [iterations 1000000
        groups     10]
    (testing "single-thread v7nc uuid uniqueness..."
      (dotimes [_ groups]
        (let [result (repeatedly iterations v7nc)]
          (is (= (count result) (count (set result)))))))))

(deftest check-v7nc-concurrency
  (doseq [concur (range 2 9)]
    (let [extent    1000000
          agents    (map agent (repeat concur nil))
          working   (map #(send-off %
                            (fn [state]
                              (repeatedly extent v7nc)))
                      agents)
          _         (apply await working)
          answers   (map deref working)]
      (testing (str "concurrent v7nc uuid uniqueness (" concur " threads)...")
        (is (= (* concur extent)
               (count (apply clojure.set/union (map set answers))))))

      (testing (str "concurrent v7nc monotonic increasing (" concur " threads)...")
        (is (every? identity
                    (map (partial apply uuid/<) answers)))))))

(deftest check-v7nc-timestamp
  (testing "v7nc timestamp round-trip"
    (dotimes [_ 100000]
      (let [before (System/currentTimeMillis)
            u      (v7nc)
            after  (System/currentTimeMillis)]
        (is (<= before (uuid/get-unix-time u) after))))))
