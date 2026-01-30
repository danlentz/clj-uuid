(ns clj-uuid.bitmop2-test
  (:require
    [clojure.test     :refer :all]
    [clj-uuid.bitmop  :as b1]
    [clj-uuid.bitmop2 :as b2 :refer :all])
  (:import [java.nio ByteBuffer]
           [java.util UUID]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 1. Correctness -- Carried-Forward Primitives
;;
;; These mirror the existing bitmop_test.clj tests exactly, exercising
;; bitmop2's implementations of mask, ldb, dpb, bit-count, byte casts,
;; byte reassembly, and hex conversion.
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
;; 2. Cross-Validation -- bitmop vs bitmop2
;;
;; Verify that bitmop2's reimplemented functions produce exactly the
;; same results as bitmop for a range of inputs including edge cases.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest check-cross-validation-mask
  (testing "mask: bitmop vs bitmop2 produce identical results..."
    (doseq [w (range 0 65)
            o (range 0 65)
            :when (<= (+ w o) 64)]
      (is (= (b1/mask w o) (b2/mask w o))
        (format "mask(%d, %d)" w o)))))


(deftest check-cross-validation-ldb-dpb
  (testing "ldb: bitmop vs bitmop2 produce identical results..."
    (let [test-values [0 1 -1 0x0F 0xFF 0xFFFF 0xFFFFFFFF
                       0x7FFFFFFFFFFFFFFF -2 (unchecked-long 0xDEADBEEFCAFEBABE)]]
      (doseq [m [(b2/mask 4 0) (b2/mask 8 8) (b2/mask 16 16) (b2/mask 32 0)
                 (b2/mask 32 32) (b2/mask 48 0) (b2/mask 12 48) (b2/mask 4 60)
                 (b2/mask 64 0) (b2/mask 63 1) (b2/mask 2 62)]
              v test-values]
        (is (= (b1/ldb m v) (b2/ldb m v))
          (format "ldb(mask, 0x%016X)" v)))))
  (testing "dpb: bitmop vs bitmop2 produce identical results..."
    (let [test-values [0 1 -1 0xFF 0xFFFF (unchecked-long 0xDEADBEEFCAFEBABE)]]
      (doseq [m [(b2/mask 4 0) (b2/mask 4 12) (b2/mask 8 48) (b2/mask 2 62)
                 (b2/mask 16 16) (b2/mask 32 32) (b2/mask 64 0)]
              v test-values
              deposit [0 1 0x3 0x7 0xF 0xFF]]
        (is (= (b1/dpb m v deposit) (b2/dpb m v deposit))
          (format "dpb(mask, 0x%016X, 0x%016X)" v deposit))))))


(deftest check-cross-validation-long-bytes
  (testing "long->bytes: bitmop vs bitmop2 produce identical byte arrays..."
    (let [test-values [0 1 -1 0xFF 0xFFFF 0xFFFFFFFF
                       0x7FFFFFFFFFFFFFFF Long/MIN_VALUE
                       (unchecked-long 0xDEADBEEFCAFEBABE) 0x0123456789ABCDEF]]
      (doseq [v test-values]
        (is (= (seq (b1/long->bytes v)) (seq (b2/long->bytes v)))
          (format "long->bytes(0x%016X)" v)))))
  (testing "bytes->long: bitmop vs bitmop2 produce identical longs..."
    (dotimes [_ 100]
      (let [arr (byte-array (repeatedly 16 #(unchecked-byte (rand-int 256))))]
        (is (= (b1/bytes->long arr 0) (b2/bytes->long arr 0)))
        (is (= (b1/bytes->long arr 8) (b2/bytes->long arr 8))))))
  (testing "assemble-bytes: bitmop vs bitmop2 produce identical longs..."
    (dotimes [_ 256]
      (let [bytes (for [_ (range 8)]
                    (b2/sb8 (rand-int 256)))]
        (is (= (b1/assemble-bytes bytes) (b2/assemble-bytes bytes)))))))


(deftest check-cross-validation-hex
  (testing "hex: bitmop vs bitmop2 produce identical strings..."
    (let [test-values [0 1 -1 0xFF 0xFFF 0xEFEF 0xF0E0A01003
                       256 255 65536 Long/MAX_VALUE Long/MIN_VALUE]]
      (doseq [v test-values]
        (is (= (b1/hex v) (b2/hex v))
          (format "hex(0x%X)" v)))))
  (testing "octet-hex: bitmop vs bitmop2 produce identical strings..."
    (doseq [v (range 0 256)]
      (is (= (b1/octet-hex v) (b2/octet-hex v))
        (format "octet-hex(%d)" v)))))


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
      (is (= (b2/ldb (b2/mask 32 0) 0x0123456789ABCDEF)
             (ldb-buf buf 0 (b2/mask 32 0))))
      (is (= (b2/ldb (b2/mask 16 48) 0x0123456789ABCDEF)
             (ldb-buf buf 0 (b2/mask 16 48))))
      (is (= (b2/ldb (b2/mask 4 12) 0x0123456789ABCDEF)
             (ldb-buf buf 0 (b2/mask 4 12))))
      ;; Extract from LSB (word offset 8)
      (is (= (b2/ldb (b2/mask 48 0) (unchecked-long 0xFEDCBA9876543210))
             (ldb-buf buf 8 (b2/mask 48 0))))
      (is (= (b2/ldb (b2/mask 2 62) (unchecked-long 0xFEDCBA9876543210))
             (ldb-buf buf 8 (b2/mask 2 62))))))
  (testing "dpb-buf deposits fields into buffer words..."
    (let [buf (buffer -1 -1)]
      ;; Deposit version bits into MSB
      (dpb-buf buf 0 (b2/mask 4 12) 0x7)
      (is (= 0x7 (ldb-buf buf 0 (b2/mask 4 12))))
      ;; Deposit variant bits into LSB
      (dpb-buf buf 8 (b2/mask 2 62) 0x2)
      (is (= 0x2 (ldb-buf buf 8 (b2/mask 2 62))))
      ;; Non-targeted bits are preserved
      (is (= (b2/ldb (b2/mask 12 0) -1)
             (ldb-buf buf 0 (b2/mask 12 0))))
      (is (= (b2/ldb (b2/mask 48 0) (b2/dpb (b2/mask 2 62) -1 0x2))
             (ldb-buf buf 8 (b2/mask 48 0))))))
  (testing "dpb-buf returns the buffer for chaining..."
    (let [buf (buffer)]
      (is (identical? buf (dpb-buf buf 0 (b2/mask 4 12) 0x1))))))


(deftest check-buffer-conversion
  (testing "buf->bytes produces correct 16-byte array..."
    (let [buf (buffer 0x0123456789ABCDEF (unchecked-long 0xFEDCBA9876543210))
          arr (buf->bytes buf)]
      (is (= 16 (alength arr)))
      (is (= 0x0123456789ABCDEF (b2/bytes->long arr 0)))
      (is (= (unchecked-long 0xFEDCBA9876543210) (b2/bytes->long arr 8)))))
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
  (testing "buf-hex matches bitmop2/hex for MSB/LSB..."
    (dotimes [_ 50]
      (let [uuid (UUID/randomUUID)
            buf  (uuid->buf uuid)
            msb  (.getMostSignificantBits uuid)
            lsb  (.getLeastSignificantBits uuid)]
        (is (= (str (b2/hex msb) (b2/hex lsb))
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
;; 4. Performance Comparison -- bitmop vs bitmop2
;;
;; Benchmarks the operations where bitmop2 replaces bitmop's shift/mask
;; loops with ByteBuffer operations:
;;   - long->bytes / bytes->long  (8-iteration loop vs single putLong/getLong)
;;   - assemble-bytes             (8-iteration dpb loop vs ByteBuffer wrap)
;;   - hex                        (map+apply+str vs StringBuilder)
;;   - UUID byte-array roundtrip  (two long->bytes + two bytes->long)
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

(defn- compare-perf
  "Benchmark bitmop (f1) vs bitmop2 (f2).  Returns {:bitmop :bitmop2 :ratio}."
  [label n f1 f2]
  (let [ns1 (bench-ns n f1)
        ns2 (bench-ns n f2)
        ratio (/ ns1 ns2)]
    (println (format "  %-30s  bitmop: %8.1f ns    bitmop2: %8.1f ns    ratio: %.2fx"
               label ns1 ns2 ratio))
    {:bitmop ns1 :bitmop2 ns2 :ratio ratio}))


(deftest check-performance-long-bytes-roundtrip
  (testing "long->bytes / bytes->long roundtrip performance..."
    (println)
    (println "--- Performance: long<->bytes ---")
    (let [test-val (unchecked-long 0xDEADBEEFCAFEBABE)]
      (compare-perf "long->bytes"
        +bench-iterations+
        #(b1/long->bytes test-val)
        #(b2/long->bytes test-val))
      (let [arr (b2/long->bytes test-val)]
        (compare-perf "bytes->long"
          +bench-iterations+
          #(b1/bytes->long arr 0)
          #(b2/bytes->long arr 0)))
      ;; roundtrip correctness under load
      (dotimes [_ 10000]
        (let [v (unchecked-long (long (* (Math/random) Long/MAX_VALUE)))]
          (is (= v (b2/bytes->long (b2/long->bytes v) 0))))))))


(deftest check-performance-assemble-bytes
  (testing "assemble-bytes performance..."
    (println)
    (println "--- Performance: assemble-bytes ---")
    (let [test-bytes (list (byte 0x01) (byte 0x02) (byte 0x03) (byte 0x04)
                           (byte 0x05) (byte 0x06) (byte 0x07) (byte 0x08))]
      (compare-perf "assemble-bytes"
        +bench-iterations+
        #(b1/assemble-bytes test-bytes)
        #(b2/assemble-bytes test-bytes))
      ;; correctness under load
      (dotimes [_ 10000]
        (let [bytes (for [_ (range 8)] (b2/sb8 (rand-int 256)))]
          (is (= (b1/assemble-bytes bytes) (b2/assemble-bytes bytes))))))))


(deftest check-performance-hex
  (testing "hex conversion performance..."
    (println)
    (println "--- Performance: hex ---")
    (let [test-val (unchecked-long 0xDEADBEEFCAFEBABE)]
      (compare-perf "hex (long)"
        +bench-iterations+
        #(b1/hex test-val)
        #(b2/hex test-val))
      ;; correctness under load
      (dotimes [_ 10000]
        (let [v (unchecked-long (long (* (Math/random) Long/MAX_VALUE)))]
          (is (= (b1/hex v) (b2/hex v))))))))


(deftest check-performance-uuid-byte-array-roundtrip
  (testing "UUID byte-array roundtrip performance..."
    (println)
    (println "--- Performance: UUID byte-array roundtrip ---")
    (let [msb 0x0123456789ABCDEF
          lsb (unchecked-long 0xFEDCBA9876543210)
          uuid (UUID. msb lsb)]
      ;; bitmop approach: long->bytes for each half, bytes->long to reassemble
      (compare-perf "UUID -> byte[] -> UUID"
        +bench-iterations+
        (fn []
          (let [arr (byte-array 16)]
            (b1/long->bytes (.getMostSignificantBits uuid) arr 0)
            (b1/long->bytes (.getLeastSignificantBits uuid) arr 8)
            (UUID. (b1/bytes->long arr 0) (b1/bytes->long arr 8))))
        (fn []
          (let [arr (byte-array 16)]
            (b2/long->bytes (.getMostSignificantBits uuid) arr 0)
            (b2/long->bytes (.getLeastSignificantBits uuid) arr 8)
            (UUID. (b2/bytes->long arr 0) (b2/bytes->long arr 8)))))
      ;; buffer approach: direct buffer conversion (no byte array intermediate)
      (println (format "  %-30s                                bitmop2: %8.1f ns"
                 "UUID -> buf -> UUID (buffer)"
                 (bench-ns +bench-iterations+
                   #(buf->uuid (uuid->buf uuid)))))
      ;; correctness under load
      (dotimes [_ 10000]
        (let [u (UUID/randomUUID)]
          (is (= u (buf->uuid (uuid->buf u)))))))))


(deftest check-performance-buf-hex-vs-to-string
  (testing "buf-str vs UUID.toString performance..."
    (println)
    (println "--- Performance: UUID string rendering ---")
    (let [uuid (UUID. 0x0123456789ABCDEF (unchecked-long 0xFEDCBA9876543210))
          buf  (uuid->buf uuid)]
      (compare-perf "UUID string rendering"
        +bench-iterations+
        #(.toString uuid)
        #(buf-str buf))
      ;; correctness under load
      (dotimes [_ 1000]
        (let [u   (UUID/randomUUID)
              b   (uuid->buf u)]
          (is (= (.toUpperCase (.toString u)) (buf-str b))))))))


(deftest check-performance-summary
  (testing "performance summary..."
    (println)
    (println "=== Performance Summary ===")
    (println)
    (let [test-val   (unchecked-long 0xDEADBEEFCAFEBABE)
          test-bytes (list (byte 1) (byte 2) (byte 3) (byte 4)
                           (byte 5) (byte 6) (byte 7) (byte 8))
          test-arr   (b2/long->bytes test-val)
          test-uuid  (UUID. test-val (unchecked-long 0xCAFEBABEDEADBEEF))
          test-buf   (uuid->buf test-uuid)
          n          +bench-iterations+
          results    [(compare-perf "long->bytes"     n
                        #(b1/long->bytes test-val)
                        #(b2/long->bytes test-val))
                      (compare-perf "bytes->long"     n
                        #(b1/bytes->long test-arr 0)
                        #(b2/bytes->long test-arr 0))
                      (compare-perf "assemble-bytes"  n
                        #(b1/assemble-bytes test-bytes)
                        #(b2/assemble-bytes test-bytes))
                      (compare-perf "hex"             n
                        #(b1/hex test-val)
                        #(b2/hex test-val))]]
      (println)
      (println (format "  Average speedup ratio: %.2fx"
                 (/ (reduce + (map :ratio results)) (count results))))
      (println))))
