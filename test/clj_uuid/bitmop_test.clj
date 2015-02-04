(ns clj-uuid.bitmop-test
  (:require
    [clojure.test    :refer :all]
    [clj-uuid.bitmop :refer :all]))

(comment
  
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
;;; example of DPB test result
;;
;;   "ffffffffffff01"
;;   "ffffffffff02ff"
;;   "ffffffff04ffff"
;;   "ffffff08ffffff"
;;   "ffff10ffffffff"
;;   "ff20ffffffffff"
;;   "40ffffffffffff"


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



(deftest check-byte-vector-representation
  (testing "raw conversion..."      
    (is (= (long-to-octets -1)                 [0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff]))
    (is (= (long-to-octets 1)                  [0 0 0 0 0 0 0 1]))
    (is (= (long-to-octets 1 1)                [1]))
    (is (= (long-to-octets 1 2)                [0 1]))
    (is (= (long-to-octets 0x100f 0)           [16 15]))
    (is (= (long-to-octets 0x7f3f302f201f100f) [127 63 48 47 32 31 16 15]))
    (is (= (long-to-octets (mask 1 63))        [128 0 0 0 0 0 0 0]))
    (is (= (long-to-octets (inc (mask 4 60)))  [240 0 0 0 0 0 0 1])))
  (testing "sbvector representation..."    
    (is (every? #(= % Byte) (map type (sbvec -1))))
    (is (= (sbvec (long-to-octets (inc (mask 4 60)))) (sbvec (inc (mask 4 60)))))
    (is (= (sbvec -1)               [-1  -1  -1  -1  -1  -1  -1  -1]))    
    (is (= (sbvec -256)             [-1  -1  -1  -1  -1  -1  -1  0]))
    (is (= (sbvec 0)                [ 0   0   0   0   0   0   0   0]))
    (is (= (sbvec 255)              [ 0   0   0   0   0   0   0   -1]))
    (is (= (sbvec 0x100)            [ 0   0   0   0   0   0   1   0]))
    (is (= (sbvec 0x3ff)            [ 0   0   0   0   0   0   3   -1]))
    (is (= (sbvec 0x400)            [ 0   0   0   0   0   0   4   0]))
    (is (= (sbvec 0xfff)            [ 0   0   0   0   0   0   15  -1]))
    (is (= (sbvec 0x1000)           [ 0   0   0   0   0   0   16  0]))
    (is (= (sbvec 0x1fff)           [ 0   0   0   0   0   0   31  -1]))
    (is (= (sbvec 0x2000)           [ 0   0   0   0   0   0   32  0]))
    (is (= (sbvec 0x3fff)           [ 0   0   0   0   0   0   63  -1]))
    (is (= (sbvec 0x4000)           [ 0   0   0   0   0   0   64  0]))
    (is (= (sbvec 0x7fff)           [ 0   0   0   0   0   0  127  -1]))
    (is (= (sbvec 0x8000)           [ 0   0   0   0   0   0 -128  0]))
    (is (= (sbvec 0xffff)           [ 0   0   0   0   0   0   -1  -1]))
    (is (= (sbvec 0x10000)          [ 0   0   0   0   0   1   0   0]))
    (is (= (sbvec 0x1ffff)          [ 0   0   0   0   0   1   -1  -1]))
    (is (= (sbvec 0x20000)          [ 0   0   0   0   0   2   0   0]))
    (is (= (sbvec 0x3ffff)          [ 0   0   0   0   0   3   -1  -1]))
    (is (= (sbvec 0x40000)          [ 0   0   0   0   0   4   0   0]))
    (is (= (sbvec 0x7ffff)          [ 0   0   0   0   0   7   -1  -1]))
    (is (= (sbvec 0x80000)          [ 0   0   0   0   0   8   0   0]))
    (is (= (sbvec 0xfffff)          [ 0   0   0   0   0   15  -1  -1]))
    (is (= (sbvec 0x100000)         [ 0   0   0   0   0   16  0   0]))
    (is (= (sbvec 0x1fffff)         [ 0   0   0   0   0   31  -1  -1]))
    (is (= (sbvec 0x200000)         [ 0   0   0   0   0   32  0   0]))
    (is (= (sbvec 0x3fffff)         [ 0   0   0   0   0   63  -1  -1]))
    (is (= (sbvec 0x400000)         [ 0   0   0   0   0   64  0   0]))
    (is (= (sbvec 0x7fffff)         [ 0   0   0   0   0  127  -1  -1])) 
    (is (= (sbvec 0x800000)         [ 0   0   0   0   0 -128  0   0]))
    (is (= (sbvec 0xffffff)         [ 0   0   0   0   0   -1  -1  -1]))
    (is (= (sbvec 0x1000000)        [ 0   0   0   0   1   0   0   0]))
    (is (= (sbvec 0x1ffffff)        [ 0   0   0   0   1   -1  -1  -1]))
    (is (= (sbvec 0x2000000)        [ 0   0   0   0   2   0   0   0]))
    (is (= (sbvec 0x2ffffff)        [ 0   0   0   0   2   -1  -1  -1]))
    (is (= (sbvec 0x3000000)        [ 0   0   0   0   3   0   0   0]))
    (is (= (sbvec 0x3fefefe)        [ 0   0   0   0   3   -2  -2  -2]))
    (is (= (sbvec 0x400000000)      [ 0   0   0   4   0   0   0   0]))
    (is (= (sbvec 0x4ffffffff)      [ 0   0   0   4   -1  -1  -1  -1]))
    (is (= (sbvec 0x800000000)      [ 0   0   0   8   0   0   0   0]))
    (is (= (sbvec 0x80a0a0a0a)      [ 0   0   0   8   10  10  10  10]))
    (is (= (sbvec 0xa0a0a0a0a)      [ 0   0   0   10  10  10  10  10]))
    (is (= (sbvec 0xfffffffff)      [ 0   0   0   15  -1  -1  -1  -1])))
  (testing "ubvector representation..."
    (is (= (ubvec (inc (mask 4 60))) [240 0 0 0 0 0 0 1]))
    (is (= (ubvec (ubvec -1))        [255 255 255 255 255 255 255 255]))
    (is (= (ubvec -1)               [255 255 255 255 255 255 255 255]))
    (is (= (ubvec -256)             [255 255 255 255 255 255 255  0]))
    (is (= (ubvec 0)                [ 0   0   0   0   0   0   0   0]))
    (is (= (ubvec 255)              [ 0   0   0   0   0   0   0  255]))
    (is (= (ubvec 0x100)            [ 0   0   0   0   0   0   1   0]))
    (is (= (ubvec 0x3ff)            [ 0   0   0   0   0   0   3  255]))
    (is (= (ubvec 0x400)            [ 0   0   0   0   0   0   4   0]))
    (is (= (ubvec 0xfff)            [ 0   0   0   0   0   0   15 255]))     
    (is (= (ubvec 0x1000)           [ 0   0   0   0   0   0   16  0]))
    (is (= (ubvec 0x1fff)           [ 0   0   0   0   0   0   31 255]))
    (is (= (ubvec 0x2000)           [ 0   0   0   0   0   0   32  0]))
    (is (= (ubvec 0x3fff)           [ 0   0   0   0   0   0   63 255]))
    (is (= (ubvec 0x4000)           [ 0   0   0   0   0   0   64  0]))
    (is (= (ubvec 0x7fff)           [ 0   0   0   0   0   0  127 255]))
    (is (= (ubvec 0x8000)           [ 0   0   0   0   0   0  128  0]))
    (is (= (ubvec 0xffff)           [ 0   0   0   0   0   0  255 255]))
    (is (= (ubvec 0x10000)          [ 0   0   0   0   0   1   0   0]))
    (is (= (ubvec 0x1ffff)          [ 0   0   0   0   0   1  255 255]))
    (is (= (ubvec 0x20000)          [ 0   0   0   0   0   2   0   0]))
    (is (= (ubvec 0x3ffff)          [ 0   0   0   0   0   3  255 255]))
    (is (= (ubvec 0x40000)          [ 0   0   0   0   0   4   0   0]))
    (is (= (ubvec 0x7ffff)          [ 0   0   0   0   0   7  255 255]))
    (is (= (ubvec 0x80000)          [ 0   0   0   0   0   8   0   0]))
    (is (= (ubvec 0xfffff)          [ 0   0   0   0   0   15 255 255]))
    (is (= (ubvec 0x100000)         [ 0   0   0   0   0   16  0   0]))
    (is (= (ubvec 0x1fffff)         [ 0   0   0   0   0   31 255 255]))
    (is (= (ubvec 0x200000)         [ 0   0   0   0   0   32  0   0]))
    (is (= (ubvec 0x3fffff)         [ 0   0   0   0   0   63 255 255]))
    (is (= (ubvec 0x400000)         [ 0   0   0   0   0   64  0   0]))
    (is (= (ubvec 0x7fffff)         [ 0   0   0   0   0  127 255 255])) 
    (is (= (ubvec 0x800000)         [ 0   0   0   0   0  128  0   0]))
    (is (= (ubvec 0xffffff)         [ 0   0   0   0   0  255 255 255]))
    (is (= (ubvec 0x1000000)        [ 0   0   0   0   1   0   0   0]))
    (is (= (ubvec 0x1ffffff)        [ 0   0   0   0   1  255 255 255]))
    (is (= (ubvec 0x2000000)        [ 0   0   0   0   2   0   0   0]))
    (is (= (ubvec 0x2ffffff)        [ 0   0   0   0   2  255 255 255]))
    (is (= (ubvec 0x3000000)        [ 0   0   0   0   3   0   0   0]))
    (is (= (ubvec 0x3fefefe)        [ 0   0   0   0   3  254 254 254]))
    (is (= (ubvec 0x400000000)      [ 0   0   0   4   0   0   0   0]))
    (is (= (ubvec 0x4ffffffff)      [ 0   0   0   4  255 255 255 255]))
    (is (= (ubvec 0x800000000)      [ 0   0   0   8   0   0   0   0]))
    (is (= (ubvec 0x80a0a0a0a)      [ 0   0   0   8   10  10  10  10]))
    (is (= (ubvec 0xa0a0a0a0a)      [ 0   0   0   10  10  10  10  10]))
    (is (= (ubvec 0xfffffffff)      [ 0   0   0   15 255 255 255 255]))))


(deftest check-byte-reassembly-roundtrip
  (testing "dissasemble/reassemble-bytes..."      
    (dotimes [_ 256]
      (let [bytes (for [i (range 8)]
                    (sb8 (rand-int (mask 8 0))))] 
        (is (= (sbvec (assemble-bytes bytes)) bytes))))))


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
    (is (= (hex (ubvec 0xfff))                     "0000000000000FFF"))
    (is (= (hex 0xefef)                            "000000000000EFEF"))
    (is (= (hex (ubvec 0xefef))                    "000000000000EFEF"))
    (is (= (hex 0xf0e0a01003)                      "000000F0E0A01003"))
    (is (= (hex (ubvec 0xf0e0a01003))              "000000F0E0A01003"))
    (is (= (hex (ubvec 0xfff))                     "0000000000000FFF"))
    (is (= (hex (ubvec 0xefef))                    "000000000000EFEF"))
    (is (= (hex (ubvec 0xf0e0a01003))              "000000F0E0A01003"))
    (is (= (hex -1)                                "FFFFFFFFFFFFFFFF"))
    (is (= (hex (ubvec -1))                        "FFFFFFFFFFFFFFFF"))
    (is (= (hex 256)                               "0000000000000100"))
    (is (= (hex 255)                               "00000000000000FF"))
    (is (= (hex 65536)                             "0000000000010000"))
    (is (= (hex -1)                                "FFFFFFFFFFFFFFFF"))
    (is (= (hex (ubvec (inc (mask 4 60))))         "F000000000000001")))
  (testing "hex string unconversion..."
    (is (= (unhex "0000000000000100") 256))
    (is (= (unhex "00000000000000FF") 255))
    (is (= (unhex "0000000000010000") 65536))
    (is (= (unhex "FFFFFFFFFFFFFFFF") -1))
    (is (= (unhex "F000000000000001") (inc (mask 4 60))))))


)
