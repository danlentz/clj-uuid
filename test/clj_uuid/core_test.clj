(ns clj-uuid.core-test
  "Tests that exercise the clj-uuid.core namespace directly.
  Mirrors api-test but requires from clj-uuid.core instead of clj-uuid."
  (:refer-clojure :exclude [uuid? max])
  (:require [clojure.test    :refer :all]
            [clojure.string  :as str]
            [clj-uuid.core   :refer :all :exclude [= > <]]
            [clj-uuid.core   :as core]
            [clj-uuid.clock  :as clock])
  (:import
   (java.lang IllegalArgumentException)
   (java.net  URI URL)
   (java.util Date UUID)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Protocol Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-null-uuid-protocol
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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Constructor Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-v0-null-constructor
  (is (= +null+ (null)))
  (is (= +null+ (v0))))

(deftest check-max-constructor
  (is (= +max+ (max)))
  (is (not= (null) (max))))

(deftest check-v1-constructor
  (testing "v1 produces a valid version-1 UUID"
    (let [u (v1)]
      (is (instance? UUID u))
      (is (= 1 (get-version u)))
      (is (= 2 (get-variant u))))))

(deftest check-v1-uniqueness
  (testing "v1 UUIDs are unique"
    (let [uuids (repeatedly 10000 v1)]
      (is (= 10000 (count (set uuids)))))))

(deftest check-v3-constructor
  (testing "v3 produces deterministic UUID"
    (let [u1 (v3 +namespace-dns+ "example.com")
          u2 (v3 +namespace-dns+ "example.com")]
      (is (= u1 u2))
      (is (= 3 (get-version u1)))
      (is (= 2 (get-variant u1)))))

  (testing "different inputs produce different UUIDs"
    (is (not= (v3 +namespace-dns+ "a")
              (v3 +namespace-dns+ "b")))))

(deftest check-v4-constructor
  (testing "v4 zero-arity produces random UUID with correct bits"
    (dotimes [_ 1000]
      (let [u (v4)]
        (is (= 4 (get-version u)))
        (is (= 2 (get-variant u))))))

  (testing "v4 two-arity stamps version and variant"
    (is (= 4 (get-version (v4 0 0))))
    (is (= 2 (get-variant (v4 0 0))))
    (is (= 4 (get-version (v4 -1 -1))))
    (is (= 2 (get-variant (v4 -1 -1))))))

(deftest check-v5-constructor
  (testing "v5 produces deterministic UUID"
    (let [u1 (v5 +namespace-dns+ "example.com")
          u2 (v5 +namespace-dns+ "example.com")]
      (is (= u1 u2))
      (is (= 5 (get-version u1)))
      (is (= 2 (get-variant u1)))))

  (testing "v3 and v5 produce different results for same input"
    (is (not= (v3 +namespace-dns+ "example.com")
              (v5 +namespace-dns+ "example.com")))))

(deftest check-v6-constructor
  (testing "v6 produces a valid version-6 UUID"
    (let [u (v6)]
      (is (instance? UUID u))
      (is (= 6 (get-version u)))
      (is (= 2 (get-variant u))))))

(deftest check-v6-uniqueness
  (testing "v6 UUIDs are unique"
    (let [uuids (repeatedly 10000 v6)]
      (is (= 10000 (count (set uuids)))))))

(deftest check-v7-constructor
  (testing "v7 produces valid version-7 UUID"
    (let [u (v7)]
      (is (instance? UUID u))
      (is (= 7 (get-version u)))
      (is (= 2 (get-variant u))))))

(deftest check-v7-uniqueness
  (testing "v7 UUIDs are unique"
    (let [uuids (repeatedly 10000 v7)]
      (is (= 10000 (count (set uuids)))))))

(deftest check-v8-constructor
  (testing "v8 stamps version=8 and variant=2"
    (is (= 8 (get-version (v8 0 0))))
    (is (= 2 (get-variant (v8 0 0))))
    (is (= 8 (get-version (v8 -1 -1))))
    (is (= 2 (get-variant (v8 -1 -1))))))

(deftest check-squuid-constructor
  (testing "squuid returns a v4 UUID"
    (let [s (squuid)]
      (is (uuid? s))
      (is (= 4 (get-version s)))
      (is (= 2 (get-variant s)))))

  (testing "squuids are unique"
    (let [uuids (repeatedly 1000 squuid)]
      (is (= 1000 (count (set uuids)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Predicates and Coercion
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-uuid?
  (is (true?  (uuid? (v4))))
  (is (true?  (uuid? +null+)))
  (is (true?  (uuid? +max+)))
  (is (false? (uuid? "not-a-uuid")))
  (is (false? (uuid? 42)))
  (is (false? (uuid? nil))))

(deftest check-null?-max?
  (is (true?  (null? +null+)))
  (is (false? (null? +max+)))
  (is (true?  (max? +max+)))
  (is (false? (max? +null+))))

(deftest check-string-predicates
  (is (uuid-string?     (to-string     (v4))))
  (is (uuid-urn-string? (to-urn-string (v4))))
  (is (not (uuid-string? "garbage")))
  (is (not (uuid-urn-string? "garbage")))
  (is (not (uuid-string? nil)))
  (is (not (uuid-urn-string? nil)))
  (is (not (uuid-vec? nil))))

(deftest check-uuidable?
  (is (true?  (uuidable? (v4))))
  (is (true?  (uuidable? "6ba7b810-9dad-11d1-80b4-00c04fd430c8")))
  (is (true?  (uuidable? "urn:uuid:6ba7b810-9dad-11d1-80b4-00c04fd430c8")))
  (is (true?  (uuidable? (byte-array 16))))
  (is (false? (uuidable? (byte-array 15))))
  (is (false? (uuidable? "not-a-uuid")))
  (is (false? (uuidable? nil)))
  (is (false? (uuidable? 42))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; as-uuid coercion
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-as-uuid-string
  (let [u (as-uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8")]
    (is (= +namespace-dns+ u))))

(deftest check-as-uuid-urn-string
  (let [u (as-uuid "urn:uuid:6ba7b810-9dad-11d1-80b4-00c04fd430c8")]
    (is (= +namespace-dns+ u))))

(deftest check-as-uuid-byte-array
  (let [u  (v4)
        ba (to-byte-array u)]
    (is (= u (as-uuid ba)))))

(deftest check-as-uuid-uri
  (let [uri (URI/create "urn:uuid:6ba7b810-9dad-11d1-80b4-00c04fd430c8")]
    (is (= +namespace-dns+ (as-uuid uri)))))

(deftest check-as-uuid-identity
  (let [u (v4)]
    (is (identical? u (as-uuid u)))))

(deftest check-as-uuid-errors
  (is (thrown? IllegalArgumentException (as-uuid nil)))
  (is (thrown? IllegalArgumentException (as-uuid "bad")))
  (is (thrown? IllegalArgumentException (as-uuid ""))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; as-byte-array
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-as-byte-array-types
  (testing "byte array passthrough"
    (let [ba (byte-array [1 2 3])]
      (is (identical? ba (as-byte-array ba)))))

  (testing "String produces UTF-8 bytes"
    (let [ba (as-byte-array "hello")]
      (is (= (seq (.getBytes "hello" "UTF-8")) (seq ba)))))

  (testing "URL produces bytes"
    (let [ba (as-byte-array (URL. "http://example.com"))]
      (is (pos? (alength ^bytes ba)))))

  (testing "Object produces serialized bytes"
    (let [ba (as-byte-array (Long. 42))]
      (is (pos? (alength ^bytes ba)))))

  (testing "nil throws"
    (is (thrown? IllegalArgumentException (as-byte-array nil)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Serialization
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-to-string
  (is (= "00000000-0000-0000-0000-000000000000" (to-string +null+)))
  (is (= "ffffffff-ffff-ffff-ffff-ffffffffffff" (to-string +max+))))

(deftest check-to-urn-string
  (is (= "urn:uuid:00000000-0000-0000-0000-000000000000" (to-urn-string +null+)))
  (is (= "urn:uuid:ffffffff-ffff-ffff-ffff-ffffffffffff" (to-urn-string +max+))))

(deftest check-to-hex-string
  (let [u (v4)
        h (to-hex-string u)]
    (is (= 32 (count h)))
    (is (re-matches #"[0-9A-Fa-f]{32}" h)))
  (is (= "00000000000000000000000000000000" (to-hex-string +null+)))
  (is (= "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" (to-hex-string +max+))))

(deftest check-to-uri
  (let [u   (v4)
        uri (to-uri u)]
    (is (instance? URI uri))
    (is (= u (as-uuid uri)))))

(deftest check-to-byte-array-roundtrip
  (let [u  (v4)
        ba (to-byte-array u)]
    (is (= 16 (alength ^bytes ba)))
    (is (= u (as-uuid ba)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Time accessors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-get-timestamp
  (testing "v1 has a timestamp"
    (is (some? (get-timestamp (v1)))))
  (testing "v6 has a timestamp"
    (is (some? (get-timestamp (v6)))))
  (testing "v7 has a timestamp"
    (is (some? (get-timestamp (v7)))))
  (testing "non-time-based return nil"
    (is (nil? (get-timestamp (v3 +namespace-dns+ "x"))))
    (is (nil? (get-timestamp (v4))))
    (is (nil? (get-timestamp (v5 +namespace-dns+ "x"))))
    (is (nil? (get-timestamp (v8 0 0))))))

(deftest check-get-unix-time
  (testing "v1 returns current POSIX millis"
    (let [before (System/currentTimeMillis)
          ut     (get-unix-time (v1))
          after  (System/currentTimeMillis)]
      (is (some? ut))
      (is (<= before ut after))))
  (testing "v6 returns current POSIX millis"
    (let [before (System/currentTimeMillis)
          ut     (get-unix-time (v6))
          after  (System/currentTimeMillis)]
      (is (some? ut))
      (is (<= before ut after))))
  (testing "v7 returns current POSIX millis"
    (let [before (System/currentTimeMillis)
          ut     (get-unix-time (v7))
          after  (System/currentTimeMillis)]
      (is (some? ut))
      (is (<= before ut after))))
  (testing "non-time-based return nil"
    (is (nil? (get-unix-time (v3 +namespace-dns+ "x"))))
    (is (nil? (get-unix-time (v4))))
    (is (nil? (get-unix-time (v5 +namespace-dns+ "x"))))
    (is (nil? (get-unix-time (v8 0 0))))))

(deftest check-get-instant
  (testing "v1 returns a Date"
    (let [inst (get-instant (v1))]
      (is (instance? Date inst))))
  (testing "v6 returns a Date"
    (let [inst (get-instant (v6))]
      (is (instance? Date inst))))
  (testing "v7 returns a Date"
    (let [inst (get-instant (v7))]
      (is (instance? Date inst))))
  (testing "non-time-based return nil"
    (is (nil? (get-instant (v4))))
    (is (nil? (get-instant +null+)))
    (is (nil? (get-instant +max+)))))

(deftest check-get-clk-seq
  (testing "v1 returns a clock sequence"
    (let [cs (get-clk-seq (v1))]
      (is (some? cs))
      (is (integer? cs))))
  (testing "v6 returns a clock sequence"
    (let [cs (get-clk-seq (v6))]
      (is (some? cs))
      (is (<= 0 cs 0x3FFF))))
  (testing "non-gregorian return nil"
    (is (nil? (get-clk-seq (v4))))
    (is (nil? (get-clk-seq (v7))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Comparison operators
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-multi-arity-comparisons
  (testing "single-arg always returns true"
    (let [u (v4)]
      (is (core/= u))
      (is (core/< u))
      (is (core/> u))))

  (testing "two-arg = works"
    (let [u (v3 +namespace-dns+ "same")]
      (is (core/= u u))
      (is (not (core/= u (v4))))))

  (testing "three-arg ="
    (let [u (v3 +namespace-dns+ "same")]
      (is (core/= u u u))
      (is (not (core/= u u (v4))))))

  (testing "multi-arg < with ordered UUIDs"
    (let [a (v4 0 0)
          b (v4 0x3000000000000000 0)
          c (v4 0x5000000000000000 0)]
      (is (core/< a b c))
      (is (not (core/< a c b)))))

  (testing "multi-arg > with ordered UUIDs"
    (let [a (v4 0 0)
          b (v4 0x3000000000000000 0)
          c (v4 0x5000000000000000 0)]
      (is (core/> c b a))
      (is (not (core/> c a b)))))

  (testing "< and > with equal elements returns false"
    (let [u (v4)]
      (is (not (core/< u u)))
      (is (not (core/> u u))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Field extraction
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-field-extraction
  (let [u (v1)]
    (is (integer? (get-word-high u)))
    (is (integer? (get-word-low u)))
    (is (integer? (get-time-low u)))
    (is (integer? (get-time-mid u)))
    (is (integer? (get-time-high u)))
    (is (integer? (get-clk-low u)))
    (is (integer? (get-clk-high u)))
    (is (integer? (get-node-id u)))
    (is (integer? (hash-code u)))
    (is (integer? (get-variant u)))))

(deftest check-get-version-all-types
  (is (= 0  (get-version +null+)))
  (is (= 1  (get-version (v1))))
  (is (= 3  (get-version (v3 +namespace-dns+ "x"))))
  (is (= 4  (get-version (v4))))
  (is (= 5  (get-version (v5 +namespace-dns+ "x"))))
  (is (= 6  (get-version (v6))))
  (is (= 7  (get-version (v7))))
  (is (= 8  (get-version (v8 0 0))))
  (is (= 15 (get-version +max+))))

(deftest check-get-variant-all-types
  (is (= 0 (get-variant +null+)))
  (is (= 2 (get-variant (v1))))
  (is (= 2 (get-variant (v3 +namespace-dns+ "x"))))
  (is (= 2 (get-variant (v4))))
  (is (= 2 (get-variant (v5 +namespace-dns+ "x"))))
  (is (= 2 (get-variant (v6))))
  (is (= 2 (get-variant (v7))))
  (is (= 2 (get-variant (v8 0 0))))
  (is (= 7 (get-variant +max+))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Backward compat alias
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-rfc4122-alias
  (is (= (:on-interface UUIDRfc4122) (:on-interface UUIDRfc9562))))
