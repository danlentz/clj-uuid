(ns clj-uuid.api-test
  (:refer-clojure :exclude [uuid? max])
  (:require [clojure.test   :refer :all]
            [clojure.string :as str]
            [clj-uuid       :refer :all :exclude [= > <]]
            [clj-uuid.clock :as clock])
  (:import
   (java.lang IllegalArgumentException)
   (java.net  URI URL)
   (java.util Date)))

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
    (let [tmpid (as-uuid "d9c53a66-fde2-3d04-b5ad-dce3848df07e")]
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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-variant
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-get-variant
  (testing "null UUID variant"
    (is (= 0 (get-variant +null+))))

  (testing "Leach-Salz (variant 2) for all standard versions"
    (is (= 2 (get-variant (v1))))
    (is (= 2 (get-variant (v3 +namespace-dns+ "test"))))
    (is (= 2 (get-variant (v4))))
    (is (= 2 (get-variant (v5 +namespace-dns+ "test"))))
    (is (= 2 (get-variant (v6))))
    (is (= 2 (get-variant (v7))))
    (is (= 2 (get-variant (v8 0x123 0x456)))))

  (testing "max UUID variant"
    (is (= 7 (get-variant +max+))))

  (testing "well-known namespace UUIDs are variant 2"
    (is (= 2 (get-variant +namespace-dns+)))
    (is (= 2 (get-variant +namespace-url+)))
    (is (= 2 (get-variant +namespace-oid+)))
    (is (= 2 (get-variant +namespace-x500+)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-instant
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-get-instant
  (testing "v1 returns a Date"
    (let [before (System/currentTimeMillis)
          inst   (get-instant (v1))
          after  (System/currentTimeMillis)]
      (is (instance? Date inst))
      (is (<= before (.getTime ^Date inst) after))))

  (testing "v6 returns a Date"
    (let [before (System/currentTimeMillis)
          inst   (get-instant (v6))
          after  (System/currentTimeMillis)]
      (is (instance? Date inst))
      (is (<= before (.getTime ^Date inst) after))))

  (testing "v7 returns a Date"
    (let [before (System/currentTimeMillis)
          inst   (get-instant (v7))
          after  (System/currentTimeMillis)]
      (is (instance? Date inst))
      (is (<= before (.getTime ^Date inst) after))))

  (testing "non-time-based versions return nil"
    (is (nil? (get-instant (v3 +namespace-dns+ "x"))))
    (is (nil? (get-instant (v4))))
    (is (nil? (get-instant (v5 +namespace-dns+ "x"))))
    (is (nil? (get-instant (v8 0 0))))
    (is (nil? (get-instant +null+)))
    (is (nil? (get-instant +max+)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-clk-seq
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-get-clk-seq
  (testing "v1 returns a clock sequence"
    (let [cs (get-clk-seq (v1))]
      (is (some? cs))
      (is (integer? cs))))

  (testing "v6 returns a clock sequence"
    (let [cs (get-clk-seq (v6))]
      (is (some? cs))
      (is (integer? cs))
      (is (<= 0 cs 0x3FFF) "clock sequence is 14 bits")))

  (testing "non-gregorian-time versions return nil"
    (is (nil? (get-clk-seq (v3 +namespace-dns+ "x"))))
    (is (nil? (get-clk-seq (v4))))
    (is (nil? (get-clk-seq (v5 +namespace-dns+ "x"))))
    (is (nil? (get-clk-seq (v7))))
    (is (nil? (get-clk-seq (v8 0 0)))))

  (testing "known v1 UUID clock sequence"
    ;; +namespace-x500+ is a v1 UUID with clock-seq = 180
    (is (= 180 (get-clk-seq +namespace-x500+)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; squuid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-squuid
  (testing "squuid returns a UUID"
    (is (uuid? (squuid))))

  (testing "squuid preserves v4 version bits"
    (let [s (squuid)]
      (is (= 4 (get-version s)))))

  (testing "squuid preserves variant 2"
    (let [s (squuid)]
      (is (= 2 (get-variant s)))))

  (testing "squuids are unique"
    (let [uuids (repeatedly 1000 squuid)]
      (is (= 1000 (count (set uuids))))))

  (testing "squuid embeds time in high 32 bits of MSB"
    ;; The high 32 bits of MSB come from clock/posix-time which is
    ;; derived from System/currentTimeMillis. All squuids generated
    ;; within the same ~10s window share the same high 32 bits.
    (let [uuids (repeatedly 100 squuid)
          hi32s (map #(unsigned-bit-shift-right (get-word-high %) 32)
                  uuids)]
      (is (= 1 (count (set hi32s)))
          "all squuids in a burst share the same time-derived high bits"))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; to-uri
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-to-uri
  (testing "to-uri returns a java.net.URI"
    (let [u (v4)
          uri (to-uri u)]
      (is (instance? URI uri))))

  (testing "to-uri produces correct URN format"
    (let [u   #uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
          uri (to-uri u)]
      (is (= "urn:uuid:6ba7b810-9dad-11d1-80b4-00c04fd430c8" (str uri)))))

  (testing "to-uri round-trips through as-uuid"
    (let [u   (v4)
          uri (to-uri u)]
      (is (= u (as-uuid uri)))))

  (testing "to-uri for null and max"
    (is (= "urn:uuid:00000000-0000-0000-0000-000000000000"
           (str (to-uri +null+))))
    (is (= "urn:uuid:ffffffff-ffff-ffff-ffff-ffffffffffff"
           (str (to-uri +max+))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Multi-arity comparison operators
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-multi-arity-comparisons
  (testing "single-arg always returns true"
    (let [u (v4)]
      (is (clj-uuid/= u))
      (is (clj-uuid/< u))
      (is (clj-uuid/> u))))

  (testing "two-arg = works"
    (let [u (v3 +namespace-dns+ "same")]
      (is (clj-uuid/= u u))
      (is (not (clj-uuid/= u (v4))))))

  (testing "three-arg ="
    (let [u (v3 +namespace-dns+ "same")]
      (is (clj-uuid/= u u u))
      (is (not (clj-uuid/= u u (v4))))))

  (testing "four-arg ="
    (let [u (v3 +namespace-dns+ "same")]
      (is (clj-uuid/= u u u u))
      (is (not (clj-uuid/= u u u (v4))))))

  (testing "multi-arg < with ordered UUIDs"
    ;; uuid< uses signed long comparison on MSB then LSB.
    ;; Use UUIDs whose MSBs are in signed-ascending order.
    (let [a (v4 0 0)                                      ; MSB starts with 0000...4...
          b (v4 0x3000000000000000 0)                      ; MSB starts with 3...4...
          c (v4 0x5000000000000000 0)]                     ; MSB starts with 5...4...
      (is (clj-uuid/< a b c))
      (is (not (clj-uuid/< a c b)))))

  (testing "multi-arg > with ordered UUIDs"
    (let [a (v4 0 0)
          b (v4 0x3000000000000000 0)
          c (v4 0x5000000000000000 0)]
      (is (clj-uuid/> c b a))
      (is (not (clj-uuid/> c a b)))))

  (testing "< and > with equal elements returns false"
    (let [u (v4)]
      (is (not (clj-uuid/< u u)))
      (is (not (clj-uuid/> u u))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v4 and v8 version+variant bit verification
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-v4-version-variant-bits
  (testing "v4 always has version=4 and variant=2"
    (dotimes [_ 10000]
      (let [u (v4)]
        (is (= 4 (get-version u)))
        (is (= 2 (get-variant u)))))))

(deftest check-v4-two-arg-version-variant-bits
  (testing "v4 with explicit msb/lsb always stamps version=4 and variant=2"
    (is (= 4 (get-version (v4 0 0))))
    (is (= 2 (get-variant (v4 0 0))))
    (is (= 4 (get-version (v4 -1 -1))))
    (is (= 2 (get-variant (v4 -1 -1))))
    (is (= 4 (get-version (v4 Long/MAX_VALUE Long/MIN_VALUE))))
    (is (= 2 (get-variant (v4 Long/MAX_VALUE Long/MIN_VALUE))))))

(deftest check-v8-version-variant-bits
  (testing "v8 always has version=8 and variant=2"
    (is (= 8 (get-version (v8 0 0))))
    (is (= 2 (get-variant (v8 0 0))))
    (is (= 8 (get-version (v8 -1 -1))))
    (is (= 2 (get-variant (v8 -1 -1))))
    (is (= 8 (get-version (v8 Long/MAX_VALUE Long/MIN_VALUE))))
    (is (= 2 (get-variant (v8 Long/MAX_VALUE Long/MIN_VALUE))))))

(deftest check-v7-version-variant-bits
  (testing "v7 always has version=7 and variant=2"
    (dotimes [_ 10000]
      (let [u (v7)]
        (is (= 7 (get-version u)))
        (is (= 2 (get-variant u)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; as-byte-array on various types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-as-byte-array-types
  (testing "byte array passthrough"
    (let [ba (byte-array [1 2 3 4 5])]
      (is (identical? ba (as-byte-array ba)))))

  (testing "String produces UTF-8 bytes"
    (let [ba (as-byte-array "hello")]
      (is (instance? (Class/forName "[B") ba))
      (is (= (count "hello") (alength ^bytes ba)))
      (is (= (seq (.getBytes "hello" "UTF-8")) (seq ba)))))

  (testing "URL produces bytes of string representation"
    (let [url (URL. "http://example.com")
          ba  (as-byte-array url)]
      (is (instance? (Class/forName "[B") ba))
      (is (= (seq (as-byte-array "http://example.com")) (seq ba)))))

  (testing "java.lang.Object produces serialized bytes"
    (let [ba (as-byte-array (Long. 42))]
      (is (instance? (Class/forName "[B") ba))
      (is (pos? (alength ^bytes ba)))))

  (testing "nil throws IllegalArgumentException"
    (is (thrown? IllegalArgumentException (as-byte-array nil)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; as-uuid and uuidable? from various sources
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-as-uuid-from-string
  (testing "canonical hex string"
    (let [u (as-uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8")]
      (is (= +namespace-dns+ u))))

  (testing "URN string"
    (let [u (as-uuid "urn:uuid:6ba7b810-9dad-11d1-80b4-00c04fd430c8")]
      (is (= +namespace-dns+ u))))

  (testing "invalid string throws"
    (is (thrown? IllegalArgumentException (as-uuid "not-a-uuid")))
    (is (thrown? IllegalArgumentException (as-uuid ""))))

  (testing "nil throws"
    (is (thrown? IllegalArgumentException (as-uuid nil)))))

(deftest check-as-uuid-from-byte-array
  (testing "16-byte array round-trips"
    (let [u  (v4)
          ba (to-byte-array u)]
      (is (= u (as-uuid ba)))))

  (testing "null UUID round-trips"
    (is (= +null+ (as-uuid (to-byte-array +null+)))))

  (testing "max UUID round-trips"
    (is (= +max+ (as-uuid (to-byte-array +max+))))))

(deftest check-as-uuid-from-uri
  (testing "URI with valid UUID URN"
    (let [uri (URI/create "urn:uuid:6ba7b810-9dad-11d1-80b4-00c04fd430c8")]
      (is (= +namespace-dns+ (as-uuid uri)))))

  (testing "URI round-trip through to-uri"
    (let [u (v4)]
      (is (= u (as-uuid (to-uri u)))))))

(deftest check-as-uuid-identity
  (testing "UUID passes through as-uuid unchanged"
    (let [u (v4)]
      (is (identical? u (as-uuid u))))))

(deftest check-uuidable?
  (testing "UUID is uuidable"
    (is (true? (uuidable? (v4)))))

  (testing "valid UUID strings are uuidable"
    (is (true? (uuidable? "6ba7b810-9dad-11d1-80b4-00c04fd430c8")))
    (is (true? (uuidable? "urn:uuid:6ba7b810-9dad-11d1-80b4-00c04fd430c8"))))

  (testing "invalid strings are not uuidable"
    (is (false? (uuidable? "not-a-uuid")))
    (is (false? (uuidable? ""))))

  (testing "16-byte arrays are uuidable"
    (is (true? (uuidable? (byte-array 16)))))

  (testing "wrong-length byte arrays are not uuidable"
    (is (false? (uuidable? (byte-array 15))))
    (is (false? (uuidable? (byte-array 17)))))

  (testing "URI with valid URN is uuidable"
    (is (true? (uuidable? (URI/create "urn:uuid:6ba7b810-9dad-11d1-80b4-00c04fd430c8")))))

  (testing "arbitrary objects are not uuidable"
    (is (false? (uuidable? 42)))
    (is (false? (uuidable? nil)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; uuid? on non-UUID types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-uuid?
  (testing "UUID instances return true"
    (is (true? (uuid? (v4))))
    (is (true? (uuid? +null+)))
    (is (true? (uuid? +max+))))

  (testing "non-UUID types return false"
    (is (false? (uuid? "6ba7b810-9dad-11d1-80b4-00c04fd430c8")))
    (is (false? (uuid? 42)))
    (is (false? (uuid? nil)))
    (is (false? (uuid? :keyword)))
    (is (false? (uuid? [1 2 3])))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; null? and max? edge cases
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-null?-and-max?-edge-cases
  (testing "null? only true for null UUID"
    (is (true?  (null? +null+)))
    (is (false? (null? +max+)))
    (is (false? (null? (v4))))
    (is (false? (null? (v1)))))

  (testing "max? only true for max UUID"
    (is (true?  (max? +max+)))
    (is (false? (max? +null+)))
    (is (false? (max? (v4))))
    (is (false? (max? (v1)))))

  (testing "null and max are not equal"
    (is (not (clj-uuid/= +null+ +max+))))

  (testing "comparison uses signed semantics"
    ;; uuid< compares MSB then LSB using signed longs.
    ;; max has MSB=-1, LSB=-1; null has MSB=0, LSB=0.
    ;; In signed comparison: -1 < 0, so max < null.
    (is (clj-uuid/< +max+ +null+))
    (is (clj-uuid/> +null+ +max+))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-unix-time
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-get-unix-time
  (testing "v1 returns approximate current POSIX millis"
    (let [before (System/currentTimeMillis)
          ut     (get-unix-time (v1))
          after  (System/currentTimeMillis)]
      (is (some? ut))
      (is (<= before ut after))))

  (testing "v6 returns approximate current POSIX millis"
    (let [before (System/currentTimeMillis)
          ut     (get-unix-time (v6))
          after  (System/currentTimeMillis)]
      (is (some? ut))
      (is (<= before ut after))))

  (testing "v7 returns approximate current POSIX millis"
    (let [before (System/currentTimeMillis)
          ut     (get-unix-time (v7))
          after  (System/currentTimeMillis)]
      (is (some? ut))
      (is (<= before ut after))))

  (testing "non-time-based versions return nil"
    (is (nil? (get-unix-time (v3 +namespace-dns+ "x"))))
    (is (nil? (get-unix-time (v4))))
    (is (nil? (get-unix-time (v5 +namespace-dns+ "x"))))
    (is (nil? (get-unix-time (v8 0 0))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; to-hex-string
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-to-hex-string
  (testing "produces 32-char hex string"
    (let [u (v4)
          h (to-hex-string u)]
      (is (= 32 (count h)))
      (is (re-matches #"[0-9A-Fa-f]{32}" h))))

  (testing "null UUID"
    (is (= "00000000000000000000000000000000"
           (to-hex-string +null+))))

  (testing "max UUID"
    (is (= "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
           (to-hex-string +max+))))

  (testing "hex string matches canonical string with dashes removed"
    (let [u (v4)]
      (is (= (str/upper-case (str/replace (to-string u) "-" ""))
             (to-hex-string u))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; posix-time conversion
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-posix-time-conversion
  (testing "known v1 namespace UUID timestamp"
    ;; +namespace-x500+ has get-timestamp = 131059232331511828
    ;; get-unix-time should convert to POSIX millis
    (let [ut (get-unix-time +namespace-x500+)]
      (is (some? ut))
      (is (= 886630433151 ut))))

  (testing "known v6 UUID unix time"
    (let [u  #uuid "1ef3f06f-16db-6ff0-bb01-1b50e6f39e7f"
          ut (get-unix-time u)]
      (is (= 1720648452463 ut))))

  (testing "known v7 UUID unix time"
    (let [u  #uuid "01909eae-4801-753a-bcd5-0889c34ac129"
          ut (get-unix-time u)]
      (is (= 0x01909eae4801 ut)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v0/null and max constructors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-constructors-v0-max
  (testing "null constructor"
    (is (= +null+ (null)))
    (is (= +null+ (v0))))

  (testing "max constructor"
    (is (= +max+ (max))))

  (testing "null and max are distinct"
    (is (not= (null) (max)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UUIDRfc4122 backwards compatibility alias
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-backwards-compat-alias
  (testing "UUIDRfc4122 refers to the same protocol interface as UUIDRfc9562"
    (is (= (:on-interface UUIDRfc4122) (:on-interface UUIDRfc9562)))))
