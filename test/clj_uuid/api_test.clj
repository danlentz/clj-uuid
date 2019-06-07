(ns clj-uuid.api-test
  (:refer-clojure :exclude [uuid?])
  (:require [clojure.test :refer :all]
            [clj-uuid     :refer :all])
  (:import
   (java.lang IllegalArgumentException)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Protocol Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-unique-identifier-protocol

  (testing "v0 uuid protocol..."
    (let [tmpid +null+]
      (is (= (get-word-high tmpid)       0))
      (is (= (get-word-low tmpid)        0))
      (is (= (null? tmpid)           true))
      (is (= (seq (to-byte-array tmpid)) [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (hash-code tmpid)       0))
      (is (= (get-version tmpid)         0))
      (is (= (to-string tmpid)       "00000000-0000-0000-0000-000000000000"))
      (is (=
            (to-urn-string tmpid)
            "urn:uuid:00000000-0000-0000-0000-000000000000"))
      (is (= (get-time-low tmpid)       0))
      (is (= (get-time-mid tmpid)       0))
      (is (= (get-time-high tmpid)      0))
      (is (= (get-clk-low tmpid)        0))
      (is (= (get-clk-high tmpid)       0))
      (is (= (get-node-id tmpid)        0))
      (is (= (get-timestamp tmpid)      nil))))

  (testing "v1 uuid protocol..."
    (let [tmpid +namespace-x500+]
      (is (= (get-word-high tmpid)       7757371281853190609))
      (is (= (get-word-low tmpid)        -9172705715073830712))
      (is (= (null? tmpid)           false))
      (is (=
            (seq (to-byte-array tmpid))
            [107 -89 -72 20 -99 -83 17 -47 -128 -76 0 -64 79 -44 48 -56]))
      (is (= (hash-code tmpid)       963287501))
      (is (= (get-version tmpid)         1))
      (is (= (to-string tmpid)     "6ba7b814-9dad-11d1-80b4-00c04fd430c8"))
      (is (=
            (to-urn-string tmpid)
            "urn:uuid:6ba7b814-9dad-11d1-80b4-00c04fd430c8"))
      (is (= (get-time-low tmpid)       1806153748))
      (is (= (get-time-mid tmpid)       40365))
      (is (= (get-time-high tmpid)      4561))
      (is (= (get-clk-low tmpid)        128))
      (is (= (get-clk-high tmpid)       180))
      (is (= (get-node-id tmpid)        825973027016))
      (is (= (get-timestamp tmpid)      131059232331511828))))

  (testing "v3 uuid protocol..."
    (let [tmpid (java.util.UUID/fromString
                  "d9c53a66-fde2-3d04-b5ad-dce3848df07e")]
      (is (= (get-word-high tmpid)       -2754731383046652668))
      (is (= (get-word-low tmpid)        -5355381512134070146))
      (is (= (null? tmpid)           false))
      (is (=
            (seq (to-byte-array tmpid))
            [-39 -59 58 102 -3 -30 61 4 -75 -83 -36 -29 -124 -115 -16 126]))
      (is (= (hash-code tmpid)       352791551))
      (is (= (get-version tmpid)         3))
      (is (= (to-string tmpid)       "d9c53a66-fde2-3d04-b5ad-dce3848df07e"))
      (is (=
            (to-urn-string tmpid)
            "urn:uuid:d9c53a66-fde2-3d04-b5ad-dce3848df07e"))
      (is (= (get-time-low tmpid)        3653581414))
      (is (= (get-time-mid tmpid)        64994))
      (is (= (get-time-high tmpid)       15620))
      (is (= (get-clk-low tmpid)         181))
      (is (= (get-clk-high tmpid)        173))
      (is (= (get-node-id tmpid)         242869739581566))
      (is (= (get-timestamp tmpid)       nil))))

  (testing "v4 uuid protocol..."
    (let [tmpid
          (java.util.UUID/fromString "3eb1e29a-4747-4a7d-8e40-94e245f57dc0")]
      (is (= (get-word-high tmpid)       4517641053478013565))
      (is (= (get-word-low tmpid)       -8196387622257066560))
      (is (= (null? tmpid)               false))
      (is (=
            (seq (to-byte-array tmpid))
            [62 -79 -30 -102 71 71 74 125 -114 64 -108 -30 69 -11 125 -64]))
      (is (= (hash-code tmpid)       -1304215099))
      (is (= (get-version tmpid)         4))
      (is (= (to-string tmpid)       "3eb1e29a-4747-4a7d-8e40-94e245f57dc0"))
      (is (=
            (to-urn-string tmpid)
            "urn:uuid:3eb1e29a-4747-4a7d-8e40-94e245f57dc0"))
      (is (= (get-time-low tmpid)        1051845274))
      (is (= (get-time-mid tmpid)        18247))
      (is (= (get-time-high tmpid)       19069))
      (is (= (get-clk-low tmpid)         142))
      (is (= (get-clk-high tmpid)        64))
      (is (= (get-node-id tmpid)         163699557236160))
      (is (= (get-timestamp tmpid)       nil)))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Predicate Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-predicates
  (testing "string predicates..."
    (is (uuid-string?       (to-string       (v4))))
    (is (uuid-urn-string?   (to-urn-string   (v4))))))

(deftest nil-test
  (testing "Calling certain functions/methods on nil returns nil"
    (testing "UUIDNameBytes"
      (is (thrown? IllegalArgumentException (as-byte-array nil))))

    (testing "UUIDable"
      (is (thrown? IllegalArgumentException (as-uuid nil)))
      (is (false? (uuidable? nil))))

    (testing "UUIDRfc4122"
      (is (false? (uuid? nil))))

    (is (false? (uuid-string? nil)))

    (is (false? (uuid-urn-string? nil)))

    (is (false? (uuid-vec? nil)))))

(deftest byte-array-round-trip-test
  (testing "round-trip via byte-array"
    (let [uuid #uuid "4787199e-c0e2-4609-b5b8-284f2b7d117d"]
      (is (= uuid (as-uuid (as-byte-array uuid)))))))
