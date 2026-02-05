(ns clj-uuid.random-test
  (:refer-clojure :exclude [bytes long])
  (:require [clojure.test    :refer :all]
            [clj-uuid.random :refer :all]))

(deftest check-bytes
  (testing "returns byte array of requested length"
    (doseq [n [1 4 8 16 32]]
      (let [bs (bytes n)]
        (is (= (Class/forName "[B") (class bs)))
        (is (= n (alength ^bytes bs))))))

  (testing "two calls return different bytes"
    (let [a (seq (bytes 16))
          b (seq (bytes 16))]
      (is (not= a b)
          "extremely unlikely to produce identical 128-bit sequences"))))

(deftest check-long-zero-arity
  (testing "returns a long"
    (is (instance? Long (long))))

  (testing "two calls return different values"
    (let [vals (repeatedly 100 long)]
      (is (< 90 (count (set vals)))
          "should produce mostly distinct values"))))

(deftest check-long-n-bytes
  (testing "single byte produces value in signed byte range [-128, 127]"
    ;; bytes() returns signed Java bytes; the reduce uses (+ (bit-shift-left n 8) b)
    ;; with initial accumulator 0, so a single byte gives [-128, 127]
    (dotimes [_ 1000]
      (let [v (long 1)]
        (is (<= -128 v 127)))))

  (testing "two bytes produces value in signed 2-byte range"
    ;; reduce folds: b0*256 + b1 where b0,b1 in [-128,127]
    ;; min: -128*256 + (-128) = -32896
    ;; max: 127*256 + 127 = 32639
    (dotimes [_ 1000]
      (let [v (long 2)]
        (is (<= -32896 v 32639))))))

(deftest check-eight-bits
  (testing "returns values in [0, 255]"
    (dotimes [_ 10000]
      (let [v (eight-bits)]
        (is (<= 0 v 255))))))

(deftest check-ten-bits
  (testing "returns values in [0, 1023]"
    (dotimes [_ 10000]
      (let [v (ten-bits)]
        (is (<= 0 v 1023))))))

(deftest check-eleven-bits
  (testing "returns values in [0, 2047]"
    (dotimes [_ 10000]
      (let [v (eleven-bits)]
        (is (<= 0 v 2047))))))

(deftest check-twelve-bits
  (testing "returns values in [0, 4095]"
    (dotimes [_ 10000]
      (let [v (twelve-bits)]
        (is (<= 0 v 4095))))))

(deftest check-bit-functions-distribution
  (testing "N-bit functions use full range (statistical)"
    ;; Generate enough samples to statistically confirm the high bit is used.
    ;; With 10000 samples, P(all < half-range) is vanishingly small.
    (let [n       10000
          eights  (repeatedly n eight-bits)
          tens    (repeatedly n ten-bits)
          elevens (repeatedly n eleven-bits)
          twelves (repeatedly n twelve-bits)]
      (is (some #(> % 127) eights)  "eight-bits should use bit 7")
      (is (some #(> % 511) tens)    "ten-bits should use bit 9")
      (is (some #(> % 1023) elevens) "eleven-bits should use bit 10")
      (is (some #(> % 2047) twelves) "twelve-bits should use bit 11"))))
