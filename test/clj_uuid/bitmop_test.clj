(ns clj-uuid.bitmop-test
  (:require
    [clojure.test    :refer :all]
    [clj-uuid.bitmop :refer :all]))



(deftest check-minor-arithmetic-operators
  (testing "exponentiation..."
    (letfn [(cx [n pow]
              (is (=
                    (expt n pow)
                    (loop [i pow acc 1]
                      (if (= i 0)
                        acc
                        (recur (- i 1) (* n acc)))))))
             (cb [pow]
               (is (=
                     (expt2 pow)
                     (expt 2 pow)
                     (loop [i pow acc 1]
                       (if (= i 0)
                         acc
                         (recur (- i 1) (* 2 acc)))))))]
      (repeat 10 (cx   2             (rand-int 16)))
      (repeat 10 (cx   (rand-int 16) (rand-int 16)))
      (repeat 10 (cb   (rand-int 16))))))

(deftest check-bit-mask-operators
  (testing "bit-mask ops..."
    (is (= (ub4 -1) 15))
    (is (= (ub4 16) 0))
    (is (= (ub4 15) 15))
    (is (= (ub4 7)  7))   
    (is (= (ub56 0x80) 128))
    (is (= (class (ub56 0x80)) Long))))

(deftest check-unboxed-short-vector
  (testing "octet vector representation..."  
    (is (every? #(= % Short) (map type (make-svector 8 255))))
    (is (every? #(= % Short) (map type (svector 0 1 2 3 4 5))))
    (is (every? #(= % Short) (map type (svec [0 1 2 3 4 5]))))))

(deftest check-octet-vector-conversion
  (testing "octet vector conversion..."    
    (is (every? #(= % Short) (map type (octets -1))))
    (is (= (octets -1)              [255 255 255 255 255 255 255 255]))
    (is (= (octets -256)            [255 255 255 255 255 255 255 0]))
    (is (= (octets 0)               [0]))      
    (is (= (octets 0xff)            [255]))      
    (is (= (octets 0x100)           [1 0]))
    (is (= (octets 0x3ff)           [3 255]))
    (is (= (octets 0x400)           [4 0]))
    (is (= (octets 0xfff)           [15 255]))
    (is (= (octets 0x1000)          [16 0]))
    (is (= (octets 0x1fff)          [31 255]))
    (is (= (octets 0x2000)          [32 0]))
    (is (= (octets 0x3fff)          [63 255]))
    (is (= (octets 0x4000)          [64 0]))
    (is (= (octets 0x7fff)          [127 255]))
    (is (= (octets 0x8000)          [128 0]))
    (is (= (octets 0xffff)          [255 255]))
    (is (= (octets 0x10000)         [1 0 0]))
    (is (= (octets 0x1ffff)         [1 255 255]))
    (is (= (octets 0x20000)         [2 0 0]))
    (is (= (octets 0x3ffff)         [3 255 255]))
    (is (= (octets 0x40000)         [4 0 0]))
    (is (= (octets 0x7ffff)         [7 255 255]))
    (is (= (octets 0x80000)         [8 0 0]))
    (is (= (octets 0xfffff)         [15 255 255]))
    (is (= (octets 0x100000)        [16 0 0]))
    (is (= (octets 0x1fffff)        [31 255 255]))
    (is (= (octets 0x200000)        [32 0 0]))
    (is (= (octets 0x3fffff)        [63 255 255]))
    (is (= (octets 0x400000)        [64 0 0]))
    (is (= (octets 0x7fffff)        [127 255 255])) 
    (is (= (octets 0x800000)        [128 0 0]))
    (is (= (octets 0xffffff)        [255 255 255]))
    (is (= (octets 0x1000000)       [1 0 0 0]))
    (is (= (octets 0x1ffffff)       [1 255 255 255]))
    (is (= (octets 0x2000000)       [2 0 0 0]))
    (is (= (octets 0x2ffffff)       [2 255 255 255]))
    (is (= (octets 0x3000000)       [3 0 0 0]))
    (is (= (octets 0x3fefefe)       [3 254 254 254]))
    (is (= (octets 0x400000000)     [4 0 0 0 0]))
    (is (= (octets 0x4ffffffff)     [4 255 255 255 255]))
    (is (= (octets 0x800000000)     [8 0 0 0 0]))
    (is (= (octets 0x80a0a0a0a)     [8 10 10 10 10]))
    (is (= (octets 0xa0a0a0a0a)     [10 10 10 10 10]))
    (is (= (octets 0xfffffffff)     [15 255 255 255 255]))))

(deftest check-fixed-size-word-representation
  (testing "word representation..."
    (is (every? #(= % Short) (map type (word 0xaabbccddee))))
    (is (= (word  0x3fefefe) [0 0 0 0 3 254 254 254]))
    (is (= (word -1)         [255 255 255 255 255 255 255 255]))
    (is (= (word 0xff)       [0 0 0 0 0 0 0 255]))))

(deftest check-multi-word-representation
  (testing "multi word representation..."
    (is (every? #(= % Short) (map type (words 0xff 0xaabbccddee))))
    (is (= (words 0xff 0xff) [0 0 0 0 0 0 0 255 0 0 0 0 0 0 0 255]))
    (is (= (words -1 1)      [255 255 255 255 255 255 255 255 0 0 0 0 0 0 0 1]))))

(deftest check-simple-octet-hex-mapping
  (testing "octet-hex mapping..."
    (is (= (octet-hex 0xff) "FF"))
    (is (= (octet-hex 0x00) "00"))
    (is (= (octet-hex 0x7a) "7A"))
    (is (= (octet-hex 0x11) "11"))))

(deftest check-octet-vector-to-hex-string
  (testing "octet vector to hex string conversion..."
    (is (= (hex 0xff)                              "FF"))
    (is (= (hex 0xfff)                             "0FFF"))
    (is (= (hex (octets 0xfff))                    "0FFF"))
    (is (= (hex 0xefef)                            "EFEF"))
    (is (= (hex (octets 0xefef))                   "EFEF"))
    (is (= (hex 0xf0e0a01003)                      "F0E0A01003"))
    (is (= (hex (octets 0xf0e0a01003))             "F0E0A01003"))
    (is (= (hex (word 0xfff))                      "0000000000000FFF"))
    (is (= (hex (word 0xefef))                     "000000000000EFEF"))
    (is (= (hex (word 0xf0e0a01003))               "000000F0E0A01003"))
    (is (= (hex -1)                                "FFFFFFFFFFFFFFFF"))
    (is (= (hex (octets -1))                       "FFFFFFFFFFFFFFFF"))
    (is (= (hex (word -1))                         "FFFFFFFFFFFFFFFF"))
    (is (= (hex (words 0xfff 0xfff))               "0000000000000FFF0000000000000FFF"))
    (is (= (hex (words 0xefef 0xefef))             "000000000000EFEF000000000000EFEF"))
    (is (= (hex (words 0xf0e0a01003 0xf0e0a01003)) "000000F0E0A01003000000F0E0A01003"))
    (is (= (hex (words -1 -1))                     "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"))))


