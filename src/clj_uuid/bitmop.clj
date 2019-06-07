(ns clj-uuid.bitmop
  (:refer-clojure :exclude [* + - / < > <= >= == rem bit-or bit-and bit-xor
                            bit-not bit-shift-left bit-shift-right
                            byte short int float long double inc dec
                            zero? min max true? false? unsigned-bit-shift-right])
  (:require [primitive-math :refer :all]
            [clojure.pprint :refer [cl-format pprint]]
            [clj-uuid.constants :refer :all]
            [clj-uuid.util :refer :all]))

;; NOTE: this package uses copious amounts of unchecked math on primitive
;; numeric datatypes.  These functions should be considered an internal
;; implementation detail of clj-uuid and used with appropriate external
;; checks in place.

;; Primitive Type  |  Size   |  Minimum  |     Maximum    |  Wrapper Type
;;-----------------------------------------------------------------------
;; boolean         |1?8 bits |   false   |     true       |  Boolean
;; char            | 16 bits | Unicode 0 | Unicode 2^16-1 |  Character
;; byte            |  8 bits |  -128     |     +127       |  Byte
;; short           | 16 bits |  -2^15    |     +2^15-1    |  Short
;; int             | 32 bits |  -2^31    |     +2^31-1    |  Integer
;; long            | 64 bits |  -2^63    |     +2^63-1    |  Long
;; float           | 32 bits |  IEEE754  |     IEEE754    |  Float
;; double          | 64 bits |  IEEE754  |     IEEE754    |  Double
;; void            |    ?    |     ?     |        ?       |  Void


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Simple Arithmetic Utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; {:pre [(not (neg? pow)) (< pow 64)]}

(defn expt2
  ^long
  [^long pow]
  (bit-set 0 pow))


(defn pphex [x]
  (returning x
    (cl-format *out* "~&[~A] [~64,,,'0@A]~%"
      (format "%1$016X" x)
      (Long/toBinaryString x))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Bit-masking
;;
;; So, much of the pain involved in handling UUID's correctly on the JVM
;; derives from the fact that there is no primitive unsigned numeric datatype
;; that can represent the full range of possible values of the msb and lsb.
;; Ie., we need to always deal with the unpleasant "am I negative?" approach to
;; reading (writing) that 64th bit.  To avoid the complexity of all the
;; edge cases, we encapsulate the basic primitives of working with
;; unsigned numbers entirely within the abstraction of "mask" and
;; "mask offset".  Using these, we built the two fundamental unsigned
;; bitwise operations that are used for most of the UUID calculation:
;; ldb (load-byte) and dpb (deposit-byte).
;;
;; This bitmop library is dead useful for working with unsigned binary
;; values on the JVM.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn mask
  ^long
  [^long width ^long offset]
  (if (< (+ width offset) 64)
    (bit-shift-left (dec (bit-shift-left 1 width)) offset)
    (let [x (expt2 offset)]
      (bit-and-not -1 (dec x)))))


(declare mask-offset mask-width)

(defn mask-offset
  ^long
  [^long m]
  (cond
    (zero? m) 0
    (neg?  m) (- 64 ^long (mask-width m))
    :else     (loop [c 0]
                (if (pos? (bit-and 1 (bit-shift-right m c)))
                  c
                  (recur (inc c))))))

(defn mask-width
  ^long
  [^long m]
  (if (neg? m)
    (let [x (mask-width (- (inc m)))]
      (- 64 x))
    (loop [m (bit-shift-right m (mask-offset m)) c 0]
      (if (zero? (bit-and 1 (bit-shift-right m c)))
        c
        (recur m (inc c))))))


;;;
;; (defn- ^long ctz [^long i ^long a ^long r]
;;   (if (zero? i)
;;     (if (zero? (bit-and a 1)) (inc r) r)
;;     (let [j (dec  i) j2 (expt2 j) j4 (expt2 j2)]
;;       (if (zero? ^long (bit-and  a (dec ^long j4)))
;;         (let [x  (>>>  a ^long j2)]
;;           (recur j x  (+  r ^long j2)))
;;           (recur j a r)))))
;;
;; (defn ^long mask-offset [^long m]
;;   (let [mask-low (unchecked-int m)]
;;     (if (zero? mask-low)
;;       (+ 32 (unchecked-long (ctz 5 (>>> m 32) 0)))
;;       (ctz 5 mask-low 0))))
;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LDB, DPB: Fundamental Bitwise Operations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ldb
  "Load Byte"
  ^long
  [^long bitmask ^long num]
  (let [off (mask-offset bitmask)]
    (bit-and (>>> bitmask off)
      (bit-shift-right num off))))

(defn dpb
  "Deposit Byte"
  ^long
  [^long bitmask ^long num ^long value]
  (bit-or (bit-and-not num bitmask)
    (bit-and bitmask
      (bit-shift-left value (mask-offset bitmask)))))

(defn bit-count
  ^long
  [^long x]
  (let [n (ldb #=(mask 63 0) x) s (if (neg? x) 1 0)]
    (loop [c s i 0]
      (if (zero? (bit-shift-right n i))
        c
        (recur (+ c (bit-and 1 (bit-shift-right n i))) (inc i))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Byte Casting
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ub4 [num]
  (byte (bit-and num +ub4-mask+)))

(defn ub8 [^long num]
  (unchecked-short (bit-and num +ub8-mask+)))

(defn ub16 [num]
  (int (bit-and num +ub16-mask+)))

(defn ub24 [num]
  (int (bit-and num +ub24-mask+)))

(defn ub32 [num]
  (long (bit-and num +ub32-mask+)))

(defn ub48 [num]
  (long (bit-and num +ub48-mask+)))

(defn ub56 [num]
  (long (bit-and num +ub56-mask+)))

(defn sb8 [num]
  (unchecked-byte (ub8 num)))

(defn sb16 [num]
  (unchecked-short (ub16 num)))

(defn sb32 [num]
  (unchecked-int (ub32 num)))

(defn sb64 [num]
  (unchecked-long num))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Byte (dis)Assembly
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn assemble-bytes [v]
  (loop [tot 0 bytes v c 8]
    (if (zero? c)
      tot
      (recur
        (long (dpb (mask 8 (* 8 (dec c))) tot ^long (first bytes)))
        (rest bytes)
        (dec c)))))


(defn bytes->long [^bytes arr ^long i]
  (loop [tot 0 j i c 8]
    (if (zero? c)
      tot
      (recur
        (long (dpb (mask 8 (* 8 ^long (dec c))) tot  (aget arr j)))
        (inc j)
        (dec c)))))

(defn long->bytes
  ([^long x]
   (long->bytes x (byte-array 8) 0))
  ([^long x ^bytes arr ^long i]
   (loop [j 7 k 0]
     (if (neg? j)
       arr
       (do
         (aset-byte arr (+ i k) (sb8 (ldb (mask 8 (* 8 j)) x)))
         (recur (dec j) (inc k)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hexadecimal String Representation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn octet-hex [num]
  (str
    (+hex-chars+ (bit-shift-right num 4))
    (+hex-chars+ (bit-and 0x0F num))))

(defn hex [thing]
  (if (number? thing)
    (hex (map ub8 (long->bytes thing)))
    (apply str (map octet-hex thing))))
