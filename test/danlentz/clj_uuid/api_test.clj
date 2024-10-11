(ns danlentz.clj-uuid.api-test
  (:refer-clojure :exclude [uuid? max])
  (:require [clojure.test :refer :all]
            [danlentz.clj-uuid.api  :refer :all :exclude [= > <]])
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
      (is (= (to-urn-string tmpid)
             "urn:uuid:00000000-0000-0000-0000-000000000000"))
      (is (= (get-time-low tmpid)       0))
      (is (= (get-time-mid tmpid)       0))
      (is (= (get-time-high tmpid)      0))
      (is (= (get-clk-low tmpid)        0))
      (is (= (get-clk-high tmpid)       0))
      (is (= (get-node-id tmpid)        0))
      (is (= (get-timestamp tmpid)      nil))
      (is (= (get-unix-time tmpid)      nil))))

  (testing "v1 uuid protocol..."
    (let [tmpid +namespace-x500+]
      (is (= (get-word-high tmpid)       7757371281853190609))
      (is (= (get-word-low tmpid)        -9172705715073830712))
      (is (= (null? tmpid)           false))
      (is (= (seq (to-byte-array tmpid))
             [107 -89 -72 20 -99 -83 17 -47 -128 -76 0 -64 79 -44 48 -56]))
      (is (= (hash-code tmpid)       963287501))
      (is (= (get-version tmpid)         1))
      (is (= (to-string tmpid)     "6ba7b814-9dad-11d1-80b4-00c04fd430c8"))
      (is (= (to-urn-string tmpid)
             "urn:uuid:6ba7b814-9dad-11d1-80b4-00c04fd430c8"))
      (is (= (get-time-low tmpid)       1806153748))
      (is (= (get-time-mid tmpid)       40365))
      (is (= (get-time-high tmpid)      4561))
      (is (= (get-clk-low tmpid)        128))
      (is (= (get-clk-high tmpid)       180))
      (is (= (get-node-id tmpid)        825973027016))
      (is (= (get-timestamp tmpid)      131059232331511828))
      (is (= (get-unix-time tmpid)      886630433151))))

  (testing "v3 uuid protocol..."
    (let [tmpid (java.util.UUID/fromString
                  "d9c53a66-fde2-3d04-b5ad-dce3848df07e")]
      (is (= (get-word-high tmpid)       -2754731383046652668))
      (is (= (get-word-low tmpid)        -5355381512134070146))
      (is (= (null? tmpid)           false))
      (is (= (seq (to-byte-array tmpid))
             [-39 -59 58 102 -3 -30 61 4 -75 -83 -36 -29 -124 -115 -16 126]))
      (is (= (hash-code tmpid)       352791551))
      (is (= (get-version tmpid)         3))
      (is (= (to-string tmpid)       "d9c53a66-fde2-3d04-b5ad-dce3848df07e"))
      (is (= (to-urn-string tmpid)
             "urn:uuid:d9c53a66-fde2-3d04-b5ad-dce3848df07e"))
      (is (= (get-time-low tmpid)        3653581414))
      (is (= (get-time-mid tmpid)        64994))
      (is (= (get-time-high tmpid)       15620))
      (is (= (get-clk-low tmpid)         181))
      (is (= (get-clk-high tmpid)        173))
      (is (= (get-node-id tmpid)         242869739581566))
      (is (= (get-timestamp tmpid)       nil))
      (is (= (get-unix-time tmpid)       nil))))

  (testing "v4 uuid protocol..."
    (let [tmpid #uuid "3eb1e29a-4747-4a7d-8e40-94e245f57dc0"]
      (is (= (get-word-high tmpid)       4517641053478013565))
      (is (= (get-word-low tmpid)       -8196387622257066560))
      (is (= (null? tmpid)               false))
      (is (= (seq (to-byte-array tmpid))
             [62 -79 -30 -102 71 71 74 125 -114 64 -108 -30 69 -11 125 -64]))
      (is (= (hash-code tmpid)       -1304215099))
      (is (= (get-version tmpid)         4))
      (is (= (to-string tmpid)       "3eb1e29a-4747-4a7d-8e40-94e245f57dc0"))
      (is (= (to-urn-string tmpid)
             "urn:uuid:3eb1e29a-4747-4a7d-8e40-94e245f57dc0"))
      (is (= (get-time-low tmpid)        1051845274))
      (is (= (get-time-mid tmpid)        18247))
      (is (= (get-time-high tmpid)       19069))
      (is (= (get-clk-low tmpid)         142))
      (is (= (get-clk-high tmpid)        64))
      (is (= (get-node-id tmpid)         163699557236160))
      (is (= (get-timestamp tmpid)       nil))
      (is (= (get-unix-time tmpid)       nil))))

  (testing "max uuid protocol..."
    (let [tmpid +max+]
      (is (= (get-word-high tmpid)       -1))
      (is (= (get-word-low tmpid)        -1))
      (is (= (null? tmpid)               false))
      (is (= (max? tmpid)                true))
      (is (= (seq (to-byte-array tmpid)) [-1 -1 -1 -1 -1 -1 -1 -1
                                          -1 -1 -1 -1 -1 -1 -1 -1]))
      (is (= (hash-code tmpid)           0))
      (is (= (get-version tmpid)         0xf))
      (is (= (to-string tmpid)       "ffffffff-ffff-ffff-ffff-ffffffffffff"))
      (is (= (to-urn-string tmpid)
             "urn:uuid:ffffffff-ffff-ffff-ffff-ffffffffffff"))
      (is (= (get-time-low tmpid)       0xffffffff))
      (is (= (get-time-mid tmpid)       0xffff))
      (is (= (get-time-high tmpid)      0xffff))
      (is (= (get-clk-low tmpid)        0xff))
      (is (= (get-clk-high tmpid)       0xff))
      (is (= (get-node-id tmpid)        0xffffffffffff))
      (is (= (get-timestamp tmpid)      nil))
      (is (= (get-unix-time tmpid)      nil))))

  (testing "v6 uuid protocol..."
    (let [tmpid #uuid "1ef3f06f-16db-6ff0-bb01-1b50e6f39e7f"]
      (is (= (get-word-high tmpid)       2230390600394043376))
      (is (= (get-word-low tmpid)        -4971662479354257793))
      (is (= (null? tmpid)               false))
      (is (= (max? tmpid)                false))
      (is (= (seq (to-byte-array tmpid)) [30  -13 -16 111 22  -37 111 -16
                                          -69 1   27  80  -26 -13 -98 127]))
      (is (= (hash-code tmpid)           1440357040))
      (is (= (get-version tmpid)         6))
      (is (= (to-string tmpid)       "1ef3f06f-16db-6ff0-bb01-1b50e6f39e7f"))
      (is (= (to-urn-string tmpid)
             "urn:uuid:1ef3f06f-16db-6ff0-bb01-1b50e6f39e7f"))
      (is (= (get-time-low tmpid)       0x6ff0))
      (is (= (get-time-mid tmpid)       0x16db))
      (is (= (get-time-high tmpid)      0x1ef3f06f))
      (is (= (get-clk-low tmpid)        0xbb))
      (is (= (get-clk-high tmpid)       0x1))
      (is (= (get-node-id tmpid)        0x1b50e6f39e7f))
      (is (= (get-timestamp tmpid)      0x1ef3f06f16dbff0))
      (is (= (get-unix-time tmpid)      1720648452463))))

  (testing "v7 uuid protocol..."
    (let [tmpid #uuid "01909eae-4801-753a-bcd5-0889c34ac129"]
      (is (= (get-word-high tmpid)       112764462053815610))
      (is (= (get-word-low tmpid)        -4839952836759731927))
      (is (= (null? tmpid)               false))
      (is (= (max? tmpid)                false))
      (is (= (seq (to-byte-array tmpid)) [1   -112 -98 -82  72  1  117 58
                                          -68 -43  8   -119 -61 74 -63 41]))
      (is (= (hash-code tmpid)           906895924))
      (is (= (get-version tmpid)         7))
      (is (= (to-string tmpid)       "01909eae-4801-753a-bcd5-0889c34ac129"))
      (is (= (to-urn-string tmpid)
             "urn:uuid:01909eae-4801-753a-bcd5-0889c34ac129"))
      (is (= (get-time-low tmpid)       0x01909eae))
      (is (= (get-time-mid tmpid)       0x4801))
      (is (= (get-time-high tmpid)      0x753a))
      (is (= (get-clk-low tmpid)        0xbc))
      (is (= (get-clk-high tmpid)       0xd5))
      (is (= (get-node-id tmpid)        0x0889c34ac129))
      (is (= (get-timestamp tmpid)      1720649140225))
      (is (= (get-unix-time tmpid)      0x01909eae4801))))

  (testing "v8 uuid protocol..."
    (let [tmpid #uuid "ffffffff-ffff-8fff-bfff-ffffffffffff"]
      (is (= (get-word-high tmpid)       -28673))
      (is (= (get-word-low tmpid)        -4611686018427387905))
      (is (= (null? tmpid)               false))
      (is (= (max? tmpid)                false))
      (is (= (seq (to-byte-array tmpid)) [-1  -1 -1 -1 -1 -1 -113 -1
                                          -65 -1 -1 -1 -1 -1 -1   -1]))
      (is (= (hash-code tmpid)           1073770496))
      (is (= (get-version tmpid)         8))
      (is (= (to-string tmpid)       "ffffffff-ffff-8fff-bfff-ffffffffffff"))
      (is (= (to-urn-string tmpid)
             "urn:uuid:ffffffff-ffff-8fff-bfff-ffffffffffff"))
      (is (= (get-time-low tmpid)       0xffffffff))
      (is (= (get-time-mid tmpid)       0xffff))
      (is (= (get-time-high tmpid)      0x8fff))
      (is (= (get-clk-low tmpid)        0xbf))
      (is (= (get-clk-high tmpid)       0xff))
      (is (= (get-node-id tmpid)        0xffffffffffff))
      (is (= (get-timestamp tmpid)      nil))
      (is (= (get-unix-time tmpid)      nil)))))

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
