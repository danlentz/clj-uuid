(ns clj-uuid.bitmop-test
  (:require
    [clojure.test    :refer :all]
    [clj-uuid.bitmop :refer :all]))


  
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
    (is (= 7 (ldb (mask 4 60) (mask 63 0))))
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

;;;
;; example of DPB test result
;;
;;   "ffffffffffff01"
;;   "ffffffffff02ff"
;;   "ffffffff04ffff"
;;   "ffffff08ffffff"
;;   "ffff10ffffffff"
;;   "ff20ffffffffff"
;;   "40ffffffffffff"
;;;

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
  (testing "dissasemble/reassemble-bytes..."      
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



