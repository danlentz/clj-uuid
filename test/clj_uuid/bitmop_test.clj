(ns clj-uuid.bitmop-test
  (:require
    [clojure.test    :refer :all]
    [clj-uuid.bitmop :as b :refer :all])
  (:import [java.nio ByteBuffer]
           [java.util UUID]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 1. Correctness -- Bitwise Primitives
;;
;; Tests for mask, ldb, dpb, bit-count, byte casts, byte reassembly,
;; and hex conversion.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest check-bit-mask-operators
  (testing "bit-mask construction..."
    (is (= (mask 0 0)                       0))
    (is (= (mask 0 1)                       0))
    (is (= (mask 1 0)                       1))
    (is (= (mask 2 0)                       3))
    (is (= (mask 4 0)                      15))
    (is (= (mask 8 0)                     255))
    (is (= (mask 16 0)                  65535))
    (is (= (mask 7 0)                     127))
    (is (= (mask 8 8)                   65280))
    (is (= (mask 8 4)                    4080))
    (is (= (mask 64 0)                     -1))
    (is (= (mask 63 1)                     -2))
    (is (= (mask 60 4)                    -16))
    (is (= (mask 32 0)             4294967295))
    (is (= (mask 32 16)       281474976645120))
    (is (= (mask 32 32)           -4294967296))
    (is (= (mask 3 60)    8070450532247928832))
    (is (= (mask 3 61)   -2305843009213693952))
    (is (= (mask 4 60)   -1152921504606846976))
    (is (= (mask 8  48)     71776119061217280))
    (is (= (mask 16 48)      -281474976710656))
    (is (= 3     (mask 2  0) (bit-or (mask 1  1) (mask 1 0))))
    (is (= 15    (mask 4  0) (bit-or (mask 2  2) (mask 2 0))))
    (is (= 15    (mask 4  0) (bit-or (mask 1  3) (mask 3 0))))
    (is (= 255   (mask 8  0) (bit-or (mask 4  4) (mask 4 0))))
    (is (= 255   (mask 8  0) (bit-or (mask 2  6) (mask 6 0))))
    (is (= 255   (mask 8  0) (bit-or (mask 1  7) (mask 7 0))))
    (is (= 65535 (mask 16 0) (bit-or (mask 8  8) (mask 8 0))))
    (is (= 65535 (mask 16 0) (bit-or (mask 15 1) (mask 1 0))))
    (is (= 65535 (mask 16 0) (bit-or (mask 5 11) (mask 11 0)))))
  (testing "bit-mask width computation..."
    (is (= 3    (mask-width  (mask 3 7))))
    (is (= 3    (mask-width  (mask 3 0))))
    (is (= 12   (mask-width  (mask 12 13))))
    (is (= 30   (mask-width  (mask 30 32))))
    (is (= 31   (mask-width  (mask 31 32))))
    (is (= 32   (mask-width  (mask 32 0))))
    (is (= 31   (mask-width  (mask 31 32))))
    (is (= 62   (mask-width  (mask 62 0))))
    (is (= 48   (mask-width  (mask 48 15))))
    (is (= 64   (mask-width  (mask 64 0))))
    (is (= 60   (mask-width  (mask 60 4))))
    (is (= 31   (mask-width  (mask 31 33))))
    (is (= 1    (mask-width  (mask 1  63)))))
  (testing "bit-mask offset computation..."
    (is (= 7    (mask-offset (mask 3 7))))
    (is (= 0    (mask-offset (mask 3 0))))
    (is (= 32   (mask-offset (mask 3 32))))
    (is (= 0    (mask-offset (mask 0 0))))
    (is (= 0    (mask-offset (mask 64 0))))
    (is (= 63   (mask-offset (mask 1 63))))
    (is (= 1    (mask-offset (mask 63 1))))))


(deftest check-bitwise-primitives
  (testing "ldb..."
    (is (= 15 (ldb (mask 4 0)  (mask 32 0))))
    (is (= 15 (ldb (mask 4 8)  (mask 32 0))))
    (is (= 12 (ldb (mask 4 0)  (mask 32 2))))
    (is (= -1 (ldb (mask 64 0) (mask 64 0))))
    (is (= 0x7fffffffffffffff (ldb (mask 63 1) (mask 64 0))))
    (is (= 0x0fffffffffffffff (ldb (mask 60 4) (mask 64 0))))
    (is (= 1  (ldb (mask 1 63) (mask 64 0))))
    (is (= 15 (ldb (mask 4 60) (mask 64 0))))
    (is (= 7  (ldb (mask 4 60) (mask 63 0))))
    (for [i (range 0 64)]
      (is (= 1 (ldb (mask 1 i) (mask 64 0)))))
    (for [i (range 0 61)]
      (is (= 15 (ldb (mask 4 i) (mask 64 0))))))
  (testing "dpb..."
    (map #(is (= 0x3 %))
      (for [i (range 8)]
        (ldb (mask 4 (* i 4))
          (dpb (mask 4 (* i 4)) (mask 64 0) 0x3))))
    (map #(is (= %1 %2))
      (into [] (map expt2 (range 7)))
      (for [i (range 7)]
        (ldb (mask 8 (* i 8))
          (dpb (mask 8 (* i 8)) (mask 64 0) (bit-shift-left 0x1 i))))))
  (testing "bit-count..."
    (is (= (bit-count 0x01010101)      4))
    (is (= (bit-count 0x77773333)     20))
    (is (= (bit-count (mask 17 6))    17))
    (is (= (bit-count (mask 32 8))    32))
    (is (= (bit-count (mask 56 0))    56))
    (is (= (bit-count (mask 64 0))    64))
    (is (= (bit-count (mask 63 1))    63))
    (is (= (bit-count -1)             64))
    (is (= (bit-count -0xffffffff)    33))
    (is (= (bit-count  0xffffffff)    32))
    (is (= (bit-count 0)               0))
    (is (= (bit-count (- (mask 62 0))) 3))))


(deftest check-byte-cast-operators
  (testing "byte-cast ops..."
    (is (= (sb8 255) -1))
    (is (= (sb8 127) 127))
    (is (= (sb8 -128) -128))
    (is (= (sb8 -254) 2))
    (is (= (sb64 (mask 63 1)) -2))
    (is (= (sb64 (mask 62 1)) 9223372036854775806))
    (is (= (sb64 (mask 62 2)) -4))
    (is (= (ub4 -1) 15))
    (is (= (ub4 16) 0))
    (is (= (ub4 15) 15))
    (is (= (ub4 7)  7))
    (is (= (ub56 0x80) 128))
    (is (= (class (ub56 0x80)) Long))))


(deftest check-byte-reassembly-roundtrip
  (testing "disassemble/reassemble-bytes..."
    (dotimes [_ 256]
      (let [bytes (for [i (range 8)]
                    (sb8 (rand-int (mask 8 0))))]
        (is (= (seq (long->bytes (assemble-bytes bytes))) bytes))))))


(deftest check-simple-octet-hex-mapping
  (testing "octet-hex mapping..."
    (is (= (octet-hex 0xff) "FF"))
    (is (= (octet-hex 0x00) "00"))
    (is (= (octet-hex 0x7a) "7A"))
    (is (= (octet-hex 15)   "0F"))
    (is (= (octet-hex 45)   "2D"))
    (is (= (octet-hex 250)  "FA"))
    (is (= (octet-hex 0x11) "11"))))


(deftest check-hex-string-conversion
  (testing "hex string conversion..."
    (is (= (hex 0xff)                              "00000000000000FF"))
    (is (= (hex 0xfff)                             "0000000000000FFF"))
    (is (= (hex 0xefef)                            "000000000000EFEF"))
    (is (= (hex 0xf0e0a01003)                      "000000F0E0A01003"))
    (is (= (hex -1)                                "FFFFFFFFFFFFFFFF"))
    (is (= (hex 256)                               "0000000000000100"))
    (is (= (hex 255)                               "00000000000000FF"))
    (is (= (hex 65536)                             "0000000000010000"))
    (is (= (hex -1)                                "FFFFFFFFFFFFFFFF"))))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 3. ByteBuffer-Specific Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest check-buffer-creation
  (testing "buffer 0-arity creates zeroed 16-byte buffer..."
    (let [buf (buffer)]
      (is (instance? ByteBuffer buf))
      (is (= 16 (.capacity buf)))
      (is (= 0 (.getLong buf 0)))
      (is (= 0 (.getLong buf 8)))))
  (testing "buffer 2-arity stores MSB and LSB..."
    (let [msb 0x0123456789ABCDEF
          lsb (unchecked-long 0xFEDCBA9876543210)
          ^ByteBuffer buf (buffer msb lsb)]
      (is (= msb (.getLong buf 0)))
      (is (= lsb (.getLong buf 8)))))
  (testing "buffer-from-bytes copies byte array contents..."
    (let [arr (byte-array 16)
          msb (unchecked-long 0xDEADBEEF01020304)
          lsb 0x0506070809101112]
      (doto (ByteBuffer/wrap arr)
        (.putLong 0 msb)
        (.putLong 8 lsb))
      (let [buf (buffer-from-bytes arr)]
        (is (= msb (.getLong buf 0)))
        (is (= lsb (.getLong buf 8)))
        ;; verify it's independent (not sharing backing)
        (aset-byte arr 0 0)
        (is (= msb (.getLong buf 0)))))))


(deftest check-buffer-typed-access
  (testing "put-byte / get-byte roundtrip..."
    (let [buf (buffer)]
      (doseq [i (range 16)]
        (put-byte buf i (* i 17))
        (is (= (* i 17) (get-byte buf i))
          (format "byte at offset %d" i)))))
  (testing "put-short / get-short roundtrip..."
    (let [buf (buffer)]
      (put-short buf 0  0x0102)
      (put-short buf 2  0xABCD)
      (put-short buf 14 0xFFFF)
      (is (= 0x0102 (get-short buf 0)))
      (is (= 0xABCD (get-short buf 2)))
      (is (= 0xFFFF (get-short buf 14)))))
  (testing "put-int / get-int roundtrip..."
    (let [buf (buffer)]
      (put-int buf 0  0x01020304)
      (put-int buf 4  0xDEADBEEF)
      (put-int buf 12 0xFFFFFFFF)
      (is (= 0x01020304 (get-int buf 0)))
      (is (= 0xDEADBEEF (get-int buf 4)))
      (is (= 0xFFFFFFFF (get-int buf 12)))))
  (testing "put-long / get-long roundtrip..."
    (let [buf (buffer)]
      (put-long buf 0 0x0123456789ABCDEF)
      (put-long buf 8 (unchecked-long 0xFEDCBA9876543210))
      (is (= 0x0123456789ABCDEF (get-long buf 0)))
      (is (= (unchecked-long 0xFEDCBA9876543210) (get-long buf 8)))))
  (testing "typed access reads byte-level data correctly..."
    ;; Write known bytes and verify typed reads
    (let [buf (buffer 0x0102030405060708 0x090A0B0C0D0E0F10)]
      (is (= 0x01 (get-byte buf 0)))
      (is (= 0x08 (get-byte buf 7)))
      (is (= 0x10 (get-byte buf 15)))
      (is (= 0x0102 (get-short buf 0)))
      (is (= 0x0506 (get-short buf 4)))
      (is (= 0x01020304 (get-int buf 0)))
      (is (= 0x05060708 (get-int buf 4))))))


(deftest check-buffer-word-access
  (testing "get-msb / get-lsb..."
    (let [msb 0x0123456789ABCDEF
          lsb (unchecked-long 0xFEDCBA9876543210)
          buf (buffer msb lsb)]
      (is (= msb (get-msb buf)))
      (is (= lsb (get-lsb buf)))))
  (testing "set-msb / set-lsb..."
    (let [buf (buffer)]
      (set-msb buf (unchecked-long 0xAAAAAAAAAAAAAAAA))
      (set-lsb buf 0x5555555555555555)
      (is (= (unchecked-long 0xAAAAAAAAAAAAAAAA) (get-msb buf)))
      (is (= 0x5555555555555555 (get-lsb buf)))))
  (testing "set-msb / set-lsb are independent..."
    (let [buf (buffer 0x1111111111111111 0x2222222222222222)]
      (set-msb buf (unchecked-long 0xFFFFFFFFFFFFFFFF))
      (is (= -1 (get-msb buf)))
      (is (= 0x2222222222222222 (get-lsb buf)))
      (set-lsb buf 0)
      (is (= -1 (get-msb buf)))
      (is (= 0 (get-lsb buf))))))


(deftest check-buffer-bit-field-operations
  (testing "ldb-buf extracts fields from buffer words..."
    (let [buf (buffer 0x0123456789ABCDEF (unchecked-long 0xFEDCBA9876543210))]
      ;; Extract from MSB (word offset 0)
      (is (= (b/ldb (b/mask 32 0) 0x0123456789ABCDEF)
             (ldb-buf buf 0 (b/mask 32 0))))
      (is (= (b/ldb (b/mask 16 48) 0x0123456789ABCDEF)
             (ldb-buf buf 0 (b/mask 16 48))))
      (is (= (b/ldb (b/mask 4 12) 0x0123456789ABCDEF)
             (ldb-buf buf 0 (b/mask 4 12))))
      ;; Extract from LSB (word offset 8)
      (is (= (b/ldb (b/mask 48 0) (unchecked-long 0xFEDCBA9876543210))
             (ldb-buf buf 8 (b/mask 48 0))))
      (is (= (b/ldb (b/mask 2 62) (unchecked-long 0xFEDCBA9876543210))
             (ldb-buf buf 8 (b/mask 2 62))))))
  (testing "dpb-buf deposits fields into buffer words..."
    (let [buf (buffer -1 -1)]
      ;; Deposit version bits into MSB
      (dpb-buf buf 0 (b/mask 4 12) 0x7)
      (is (= 0x7 (ldb-buf buf 0 (b/mask 4 12))))
      ;; Deposit variant bits into LSB
      (dpb-buf buf 8 (b/mask 2 62) 0x2)
      (is (= 0x2 (ldb-buf buf 8 (b/mask 2 62))))
      ;; Non-targeted bits are preserved
      (is (= (b/ldb (b/mask 12 0) -1)
             (ldb-buf buf 0 (b/mask 12 0))))
      (is (= (b/ldb (b/mask 48 0) (b/dpb (b/mask 2 62) -1 0x2))
             (ldb-buf buf 8 (b/mask 48 0))))))
  (testing "dpb-buf returns the buffer for chaining..."
    (let [buf (buffer)]
      (is (identical? buf (dpb-buf buf 0 (b/mask 4 12) 0x1))))))


(deftest check-buffer-conversion
  (testing "buf->bytes produces correct 16-byte array..."
    (let [buf (buffer 0x0123456789ABCDEF (unchecked-long 0xFEDCBA9876543210))
          arr (buf->bytes buf)]
      (is (= 16 (alength arr)))
      (is (= 0x0123456789ABCDEF (b/bytes->long arr 0)))
      (is (= (unchecked-long 0xFEDCBA9876543210) (b/bytes->long arr 8)))))
  (testing "buffer-from-bytes -> buf->bytes roundtrip..."
    (dotimes [_ 100]
      (let [arr (byte-array (repeatedly 16 #(unchecked-byte (rand-int 256))))
            buf (buffer-from-bytes arr)
            out (buf->bytes buf)]
        (is (= (seq arr) (seq out))))))
  (testing "buf->uuid produces correct UUID..."
    (let [msb 0x0123456789ABCDEF
          lsb (unchecked-long 0xFEDCBA9876543210)
          buf (buffer msb lsb)
          uuid (buf->uuid buf)]
      (is (instance? UUID uuid))
      (is (= msb (.getMostSignificantBits uuid)))
      (is (= lsb (.getLeastSignificantBits uuid)))))
  (testing "uuid->buf produces correct buffer..."
    (let [uuid (UUID/randomUUID)
          buf  (uuid->buf uuid)]
      (is (= (.getMostSignificantBits uuid) (get-msb buf)))
      (is (= (.getLeastSignificantBits uuid) (get-lsb buf)))))
  (testing "uuid->buf -> buf->uuid roundtrip..."
    (dotimes [_ 100]
      (let [uuid (UUID/randomUUID)]
        (is (= uuid (buf->uuid (uuid->buf uuid)))))))
  (testing "duplicate creates an independent copy..."
    (let [buf  (buffer (unchecked-long 0xAAAAAAAAAAAAAAAA) 0x5555555555555555)
          copy (duplicate buf)]
      (is (= (get-msb buf) (get-msb copy)))
      (is (= (get-lsb buf) (get-lsb copy)))
      ;; mutating copy doesn't affect original
      (set-msb copy 0)
      (is (= (unchecked-long 0xAAAAAAAAAAAAAAAA) (get-msb buf)))
      (is (= 0 (get-msb copy))))))


(deftest check-buffer-hex-and-string
  (testing "buf-hex produces correct 32-char hex string..."
    (let [buf (buffer 0x0123456789ABCDEF (unchecked-long 0xFEDCBA9876543210))]
      (is (= "0123456789ABCDEFFEDCBA9876543210" (buf-hex buf))))
    (let [buf (buffer 0 0)]
      (is (= "00000000000000000000000000000000" (buf-hex buf))))
    (let [buf (buffer -1 -1)]
      (is (= "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" (buf-hex buf)))))
  (testing "buf-str produces correct canonical UUID string..."
    (let [buf (buffer 0x0123456789ABCDEF (unchecked-long 0xFEDCBA9876543210))]
      (is (= "01234567-89AB-CDEF-FEDC-BA9876543210" (buf-str buf))))
    (let [buf (buffer 0 0)]
      (is (= "00000000-0000-0000-0000-000000000000" (buf-str buf)))))
  (testing "buf-str matches UUID.toString for random UUIDs..."
    (dotimes [_ 100]
      (let [uuid (UUID/randomUUID)
            buf  (uuid->buf uuid)]
        (is (= (.toUpperCase (.toString uuid)) (buf-str buf))))))
  (testing "hex->buf parses hex string correctly..."
    (let [hex-str "0123456789ABCDEFFEDCBA9876543210"
          buf     (hex->buf hex-str)]
      (is (= 0x0123456789ABCDEF (get-msb buf)))
      (is (= (unchecked-long 0xFEDCBA9876543210) (get-lsb buf)))))
  (testing "hex->buf -> buf-hex roundtrip..."
    (dotimes [_ 100]
      (let [uuid (UUID/randomUUID)
            buf  (uuid->buf uuid)
            hex1 (buf-hex buf)
            buf2 (hex->buf hex1)
            hex2 (buf-hex buf2)]
        (is (= hex1 hex2)))))
  (testing "buf-hex matches bitmop/hex for MSB/LSB..."
    (dotimes [_ 50]
      (let [uuid (UUID/randomUUID)
            buf  (uuid->buf uuid)
            msb  (.getMostSignificantBits uuid)
            lsb  (.getLeastSignificantBits uuid)]
        (is (= (str (b/hex msb) (b/hex lsb))
               (buf-hex buf)))))))


(deftest check-buffer-comparison
  (testing "buf-compare: equal buffers..."
    (is (= 0 (buf-compare (buffer 0 0) (buffer 0 0))))
    (is (= 0 (buf-compare (buffer -1 -1) (buffer -1 -1))))
    (is (= 0 (buf-compare
               (buffer 0x0123456789ABCDEF 0)
               (buffer 0x0123456789ABCDEF 0)))))
  (testing "buf-compare: MSB determines order..."
    (is (neg? (buf-compare (buffer 0 0) (buffer 1 0))))
    (is (pos? (buf-compare (buffer 1 0) (buffer 0 0))))
    ;; unsigned comparison: 0x8... is greater than 0x7... as unsigned
    (is (pos? (buf-compare
                (buffer (unchecked-long 0x8000000000000000) 0)
                (buffer 0x7FFFFFFFFFFFFFFF 0)))))
  (testing "buf-compare: LSB breaks ties..."
    (is (neg? (buf-compare (buffer 1 0) (buffer 1 1))))
    (is (pos? (buf-compare (buffer 1 1) (buffer 1 0))))
    ;; unsigned LSB comparison
    (is (pos? (buf-compare
                (buffer 0 (unchecked-long 0x8000000000000000))
                (buffer 0 0x7FFFFFFFFFFFFFFF)))))
  (testing "buf-compare: ordering matches UUID string ordering..."
    (dotimes [_ 100]
      (let [u1  (UUID/randomUUID)
            u2  (UUID/randomUUID)
            b1  (uuid->buf u1)
            b2  (uuid->buf u2)
            ;; compare canonical uppercase strings lexicographically
            str-cmp (compare (.toUpperCase (.toString u1))
                             (.toUpperCase (.toString u2)))
            buf-cmp (buf-compare b1 b2)]
        (is (= (Long/signum str-cmp) (Long/signum buf-cmp))
          (format "ordering mismatch for %s vs %s" u1 u2))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 4. Performance -- bitmop Primitives
;;
;; Benchmarks the core bitmop operations:
;;   - long->bytes / bytes->long  (ByteBuffer putLong/getLong)
;;   - assemble-bytes             (shift-accumulation loop)
;;   - hex                        (StringBuilder direct append)
;;   - UUID buffer roundtrip      (uuid->buf / buf->uuid)
;;   - buf-str vs UUID.toString   (StringBuilder vs JVM native)
;;
;; Results are printed to stdout.  No hard timing assertions are made
;; (too fragile for CI), but correctness under load is verified.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def ^:private +bench-iterations+ 100000)

(defn- bench-ns
  "Run `f` for `n` iterations after warmup.  Returns average ns/op."
  ^double [^long n f]
  ;; warmup
  (dotimes [_ (min n 10000)] (f))
  ;; timed
  (let [start (System/nanoTime)]
    (dotimes [_ n] (f))
    (double (/ (- (System/nanoTime) start) n))))

(defn- bench-print
  "Benchmark `f` and print a formatted result line.  Returns ns/op."
  [label n f]
  (let [ns-val (bench-ns n f)]
    (println (format "  %-30s  %8.1f ns" label ns-val))
    ns-val))


(deftest check-performance-long-bytes-roundtrip
  (testing "long->bytes / bytes->long performance..."
    (println)
    (println "--- Performance: long<->bytes ---")
    (let [test-val (unchecked-long 0xDEADBEEFCAFEBABE)]
      (bench-print "long->bytes" +bench-iterations+
        #(b/long->bytes test-val))
      (let [arr (b/long->bytes test-val)]
        (bench-print "bytes->long" +bench-iterations+
          #(b/bytes->long arr 0)))
      ;; roundtrip correctness under load
      (dotimes [_ 10000]
        (let [v (unchecked-long (long (* (Math/random) Long/MAX_VALUE)))]
          (is (= v (b/bytes->long (b/long->bytes v) 0))))))))


(deftest check-performance-assemble-bytes
  (testing "assemble-bytes performance..."
    (println)
    (println "--- Performance: assemble-bytes ---")
    (let [test-bytes (list (byte 0x01) (byte 0x02) (byte 0x03) (byte 0x04)
                           (byte 0x05) (byte 0x06) (byte 0x07) (byte 0x08))]
      (bench-print "assemble-bytes" +bench-iterations+
        #(b/assemble-bytes test-bytes))
      ;; correctness under load
      (dotimes [_ 10000]
        (let [bytes (for [_ (range 8)] (b/sb8 (rand-int 256)))
              result (b/assemble-bytes bytes)]
          (is (= result (b/bytes->long (byte-array (map unchecked-byte bytes)) 0))))))))


(deftest check-performance-hex
  (testing "hex conversion performance..."
    (println)
    (println "--- Performance: hex ---")
    (let [test-val (unchecked-long 0xDEADBEEFCAFEBABE)]
      (bench-print "hex (long)" +bench-iterations+
        #(b/hex test-val))
      ;; correctness under load
      (dotimes [_ 10000]
        (let [v (unchecked-long (long (* (Math/random) Long/MAX_VALUE)))]
          (is (= 16 (count (b/hex v)))))))))


(deftest check-performance-uuid-buffer-roundtrip
  (testing "UUID buffer roundtrip performance..."
    (println)
    (println "--- Performance: UUID buffer roundtrip ---")
    (let [msb 0x0123456789ABCDEF
          lsb (unchecked-long 0xFEDCBA9876543210)
          uuid (UUID. msb lsb)]
      (bench-print "UUID -> byte[] -> UUID" +bench-iterations+
        (fn []
          (let [arr (byte-array 16)]
            (b/long->bytes (.getMostSignificantBits uuid) arr 0)
            (b/long->bytes (.getLeastSignificantBits uuid) arr 8)
            (UUID. (b/bytes->long arr 0) (b/bytes->long arr 8)))))
      (bench-print "UUID -> buf -> UUID" +bench-iterations+
        #(buf->uuid (uuid->buf uuid)))
      ;; correctness under load
      (dotimes [_ 10000]
        (let [u (UUID/randomUUID)]
          (is (= u (buf->uuid (uuid->buf u)))))))))


(deftest check-performance-buf-str
  (testing "buf-str performance..."
    (println)
    (println "--- Performance: UUID string rendering ---")
    (let [uuid (UUID. 0x0123456789ABCDEF (unchecked-long 0xFEDCBA9876543210))
          buf  (uuid->buf uuid)]
      (bench-print "UUID.toString" +bench-iterations+
        #(.toString uuid))
      (bench-print "buf-str" +bench-iterations+
        #(buf-str buf))
      ;; correctness under load
      (dotimes [_ 1000]
        (let [u   (UUID/randomUUID)
              b   (uuid->buf u)]
          (is (= (.toUpperCase (.toString u)) (buf-str b))))))))
