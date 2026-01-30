(ns clj-uuid.bitmop
  "Unsigned Long and ByteBuffer-based bitwise operation primitives for
  UUID manipulation.

  Provides the same mask/ldb/dpb fundamentals as in the past, plus a
  16-byte ByteBuffer abstraction for direct UUID byte manipulation.

  The ByteBuffer approach has two key advantages:

  1. Performance: ByteBuffer provides direct typed access at byte offsets
     via single native operations (getLong, getInt, etc.) rather than
     manual shift/mask loops.
  2. Portability: The buffer abstraction maps naturally to JavaScript's
     DataView/ArrayBuffer, enabling a future cljc implementation."

  (:refer-clojure :exclude [* + - / < > <= >= == rem bit-or bit-and bit-xor
                            bit-not bit-shift-left bit-shift-right
                            byte short int float long double inc dec
                            zero? min max true? false? unsigned-bit-shift-right])
  (:require [primitive-math :refer :all]
            [clojure.pprint :refer [cl-format]]
            [clj-uuid.constants :refer :all]
            [clj-uuid.util :refer :all])
  (:import [java.nio ByteBuffer]
           [java.util UUID]))

;; NOTE: this module uses copious amounts of unchecked/primitive-math.
;; These should be considered internal implementation details.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Simple Arithmetic Utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn expt2
  "Compute 2^pow using bit-set."
  ^long
  [^long pow]
  (bit-set 0 pow))


(defn pphex
  "Pretty-print a long value in both hexadecimal and binary."
  [x]
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
;;
;; we encapsulate the basic primitives of working with
;; unsigned numbers entirely within the abstraction of "mask" and
;; "mask offset".  Using these, we built the two fundamental unsigned
;; bitwise operations that are used for most of the UUID calculation:
;; ldb (load-byte) and dpb (deposit-byte).
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mask
  "Create a bitmask of `width` bits starting at bit `offset`."
  ^long
  [^long width ^long offset]
  (if (< (+ width offset) 64)
    (bit-shift-left (dec (bit-shift-left 1 width)) offset)
    (let [x (expt2 offset)]
      (bit-and-not -1 (dec x)))))


;; Uses Long/numberOfTrailingZeros which compiles to a single TZCNT/BSF
;; instruction via JVM intrinsic.  O(1) vs the previous O(offset) loop.

(defn mask-offset
  "Return the bit offset (position of least significant set bit) of a mask."
  ^long
  [^long m]
  (if (zero? m)
    0
    (Long/numberOfTrailingZeros m)))

;; Uses Long/bitCount which compiles to a single POPCNT instruction
;; via JVM intrinsic.  O(1) vs the previous O(width) loop.

(defn mask-width
  "Return the number of set bits in a contiguous bitmask."
  ^long
  [^long m]
  (Long/bitCount m))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LDB, DPB: Fundamental Bitwise Operations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ldb
  "Load Byte -- extract the bit field defined by `bitmask` from `num`."
  ^long
  [^long bitmask ^long num]
  (let [off (mask-offset bitmask)]
    (bit-and (>>> bitmask off)
      (bit-shift-right num off))))

(defn dpb
  "Deposit Byte -- insert `value` into the bit field defined by `bitmask`
  within `num`."
  ^long
  [^long bitmask ^long num ^long value]
  (bit-or (bit-and-not num bitmask)
    (bit-and bitmask
      (bit-shift-left value (mask-offset bitmask)))))

;; Uses Long/bitCount which compiles to a single POPCNT instruction
;; via JVM intrinsic.  O(1) vs the previous O(64) loop.

(defn bit-count
  "Count the number of set bits in `x`."
  ^long
  [^long x]
  (Long/bitCount x))

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
;; Byte (dis)Assembly via ByteBuffer
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn assemble-bytes
  "Assemble a sequence of 8 bytes (big-endian) into a long."
  ^long [v]
  (loop [tot (long 0) bytes v c (int 8)]
    (if (zero? c)
      tot
      (recur
        (bit-or (bit-shift-left tot 8) (bit-and (long (first bytes)) 0xFF))
        (next bytes)
        (dec c)))))


(defn bytes->long
  "Read 8 bytes from `arr` starting at offset `i`, returning a long."
  [^bytes arr ^long i]
  (.getLong (ByteBuffer/wrap arr) (int i)))

(defn long->bytes
  "Write `x` as 8 big-endian bytes.  With one argument returns a new byte
  array.  With three arguments writes into `arr` at offset `i`."
  ([^long x]
   (long->bytes x (byte-array 8) 0))
  ([^long x ^bytes arr ^long i]
   (.putLong (ByteBuffer/wrap arr) (int i) x)
   arr))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hexadecimal String Representation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn octet-hex
  "Convert a single byte value to a two-character uppercase hex string."
  [num]
  (str
    (+hex-chars+ (bit-shift-right num 4))
    (+hex-chars+ (bit-and 0x0F num))))

(defn hex
  "Convert a long or byte sequence to a hex string.
  For a long, produces a 16-character zero-padded hex string."
  [thing]
  (if (number? thing)
    (let [^bytes arr (long->bytes (clojure.core/long thing))
          sb  (StringBuilder. 16)]
      (dotimes [i 8]
        (let [b (Byte/toUnsignedLong (aget arr (int i)))]
          (.append sb ^char (+hex-chars+ (bit-shift-right b 4)))
          (.append sb ^char (+hex-chars+ (bit-and 0x0F b)))))
      (.toString sb))
    (apply str (map octet-hex thing))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ByteBuffer Creation
;;
;; The central abstraction: a 16-byte big-endian ByteBuffer representing
;; 128 bits of UUID data.  On the JVM this is java.nio.ByteBuffer; in a
;; future cljc build this would map to DataView over an ArrayBuffer.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn buffer
  "Create a 16-byte big-endian ByteBuffer.

  0-arity:  zeroed buffer
  2-arity:  from msb and lsb longs
  The buffer uses absolute (position-independent) get/put operations."
  (^ByteBuffer []
   (ByteBuffer/allocate 16))
  (^ByteBuffer [^long msb ^long lsb]
   (doto (ByteBuffer/allocate 16)
     (.putLong 0 msb)
     (.putLong 8 lsb))))

(defn buffer-from-bytes
  "Create a 16-byte ByteBuffer from a byte array (at least 16 bytes)."
  ^ByteBuffer
  [^bytes arr]
  (let [buf (ByteBuffer/allocate 16)
        ^ByteBuffer src (ByteBuffer/wrap arr)]
    (.putLong buf 0 (.getLong src 0))
    (.putLong buf 8 (.getLong src 8))
    buf))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ByteBuffer Typed Access
;;
;; Direct typed access at byte offsets.  All operations use absolute
;; positions (no buffer position tracking).  Unsigned getter variants
;; return longs to preserve full value range on the JVM.
;;
;; These map directly to DataView methods in JavaScript:
;;   get-byte   -> DataView.getUint8
;;   get-short  -> DataView.getUint16
;;   get-int    -> DataView.getUint32
;;   get-long   -> DataView.getBigInt64
;;   put-byte   -> DataView.setUint8
;;   put-short  -> DataView.setUint16
;;   put-int    -> DataView.setUint32
;;   put-long   -> DataView.setBigInt64
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-byte
  "Read an unsigned byte (0-255) at byte offset from buffer."
  ^long [^ByteBuffer buf ^long offset]
  (Byte/toUnsignedLong (.get buf (int offset))))

(defn get-short
  "Read an unsigned 16-bit value at byte offset from buffer."
  ^long [^ByteBuffer buf ^long offset]
  (Short/toUnsignedLong (.getShort buf (int offset))))

(defn get-int
  "Read an unsigned 32-bit value at byte offset from buffer."
  ^long [^ByteBuffer buf ^long offset]
  (Integer/toUnsignedLong (.getInt buf (int offset))))

(defn get-long
  "Read a 64-bit long at byte offset from buffer."
  ^long [^ByteBuffer buf ^long offset]
  (.getLong buf (int offset)))

(defn put-byte
  "Write a byte value at byte offset in buffer.  Returns the buffer."
  ^ByteBuffer [^ByteBuffer buf ^long offset ^long val]
  (doto buf (.put (int offset) (byte val))))

(defn put-short
  "Write a 16-bit value at byte offset in buffer.  Returns the buffer."
  ^ByteBuffer [^ByteBuffer buf ^long offset ^long val]
  (doto buf (.putShort (int offset) (short val))))

(defn put-int
  "Write a 32-bit value at byte offset in buffer.  Returns the buffer."
  ^ByteBuffer [^ByteBuffer buf ^long offset ^long val]
  (doto buf (.putInt (int offset) (int val))))

(defn put-long
  "Write a 64-bit long at byte offset in buffer.  Returns the buffer."
  ^ByteBuffer [^ByteBuffer buf ^long offset ^long val]
  (doto buf (.putLong (int offset) val)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ByteBuffer Word Access (MSB/LSB)
;;
;; Convenience accessors for the two 64-bit halves of a UUID buffer.
;;   bytes 0-7:   most significant bits  (MSB)
;;   bytes 8-15:  least significant bits (LSB)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-msb
  "Read the most significant 64 bits (bytes 0-7) of a UUID buffer."
  ^long [^ByteBuffer buf]
  (.getLong buf 0))

(defn get-lsb
  "Read the least significant 64 bits (bytes 8-15) of a UUID buffer."
  ^long [^ByteBuffer buf]
  (.getLong buf 8))

(defn set-msb
  "Set the most significant 64 bits (bytes 0-7) of a UUID buffer.
  Returns the buffer."
  ^ByteBuffer [^ByteBuffer buf ^long val]
  (doto buf (.putLong 0 val)))

(defn set-lsb
  "Set the least significant 64 bits (bytes 8-15) of a UUID buffer.
  Returns the buffer."
  ^ByteBuffer [^ByteBuffer buf ^long val]
  (doto buf (.putLong 8 val)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ByteBuffer Bit Field Operations
;;
;; Buffer-aware ldb/dpb that operate on 64-bit words within the buffer.
;; The word-offset identifies which 8-byte-aligned long to operate on:
;;   0 = MSB (bytes 0-7)
;;   8 = LSB (bytes 8-15)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ldb-buf
  "Load bit field: extract bits defined by `bitmask` from the 64-bit word
  at `word-offset` in the buffer."
  ^long [^ByteBuffer buf ^long word-offset ^long bitmask]
  (ldb bitmask (.getLong buf (int word-offset))))

(defn dpb-buf
  "Deposit bit field: insert `value` into the bits defined by `bitmask`
  within the 64-bit word at `word-offset` in the buffer.  Mutates and
  returns the buffer."
  ^ByteBuffer [^ByteBuffer buf ^long word-offset ^long bitmask ^long value]
  (let [current (.getLong buf (int word-offset))
        updated (dpb bitmask current value)]
    (doto buf (.putLong (int word-offset) updated))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ByteBuffer Conversion
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn buf->bytes
  "Extract the contents of a 16-byte UUID buffer as a byte array."
  ^bytes [^ByteBuffer buf]
  (let [arr (byte-array 16)
        ^ByteBuffer dest (ByteBuffer/wrap arr)]
    (.putLong dest 0 (.getLong buf 0))
    (.putLong dest 8 (.getLong buf 8))
    arr))

(defn buf->uuid
  "Convert a 16-byte UUID buffer to a java.util.UUID."
  ^UUID [^ByteBuffer buf]
  (UUID. (.getLong buf 0) (.getLong buf 8)))

(defn uuid->buf
  "Convert a java.util.UUID to a 16-byte ByteBuffer."
  ^ByteBuffer [^UUID uuid]
  (buffer (.getMostSignificantBits uuid) (.getLeastSignificantBits uuid)))

(defn duplicate
  "Create an independent copy of a UUID buffer."
  ^ByteBuffer [^ByteBuffer buf]
  (let [copy (ByteBuffer/allocate 16)]
    (.putLong copy 0 (.getLong buf 0))
    (.putLong copy 8 (.getLong buf 8))
    copy))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ByteBuffer Hex and String Representation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- append-hex-bytes
  "Append hex characters for bytes [start, end) from buffer to StringBuilder."
  [^StringBuilder sb ^ByteBuffer buf ^long start ^long end]
  (loop [i start]
    (when (< i end)
      (let [b (Byte/toUnsignedLong (.get buf (int i)))]
        (.append sb ^char (+hex-chars+ (bit-shift-right b 4)))
        (.append sb ^char (+hex-chars+ (bit-and 0x0F b))))
      (recur (inc i)))))

(defn buf-hex
  "Convert a 16-byte UUID buffer to a 32-character hex string."
  ^String [^ByteBuffer buf]
  (let [sb (StringBuilder. 32)]
    (append-hex-bytes sb buf 0 16)
    (.toString sb)))

(defn buf-str
  "Convert a 16-byte UUID buffer to the canonical 36-character UUID string:
    xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx

  Byte ranges correspond to UUID fields:
    bytes 0-3:   time-low
    bytes 4-5:   time-mid
    bytes 6-7:   time-high-and-version
    bytes 8-9:   clock-seq
    bytes 10-15: node"
  ^String [^ByteBuffer buf]
  (let [sb (StringBuilder. 36)]
    (append-hex-bytes sb buf 0 4)
    (.append sb \-)
    (append-hex-bytes sb buf 4 6)
    (.append sb \-)
    (append-hex-bytes sb buf 6 8)
    (.append sb \-)
    (append-hex-bytes sb buf 8 10)
    (.append sb \-)
    (append-hex-bytes sb buf 10 16)
    (.toString sb)))

(defn hex->buf
  "Parse a 32-character hex string into a 16-byte UUID buffer."
  ^ByteBuffer [^String s]
  (let [buf (ByteBuffer/allocate 16)]
    (dotimes [i 16]
      (let [hi (Character/digit (.charAt s (int (* 2 i))) (int 16))
            lo (Character/digit (.charAt s (int (inc (* 2 i)))) (int 16))]
        (.put buf (int i)
          (byte (bit-or (bit-shift-left hi 4) lo)))))
    buf))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ByteBuffer Comparison
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn buf-compare
  "Lexicographic unsigned comparison of two 16-byte UUID buffers.
  Returns negative if a < b, zero if equal, positive if a > b.
  Comparison is unsigned (treats bytes as 0-255) which matches UUID
  canonical string ordering."
  ^long [^ByteBuffer a ^ByteBuffer b]
  (let [c (Long/compareUnsigned (.getLong a 0) (.getLong b 0))]
    (if (zero? c)
      (clojure.core/long (Long/compareUnsigned (.getLong a 8) (.getLong b 8)))
      (clojure.core/long c))))
