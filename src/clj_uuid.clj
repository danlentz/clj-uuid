(ns clj-uuid
  (:refer-clojure :exclude [==])
  (:require [clj-uuid
             [constants :refer :all]
             [util      :refer :all]
             [bitmop    :as bitmop]
             [clock     :as clock]
             [node      :as node]])
  (:import [java.security MessageDigest]
           [java.io       ByteArrayOutputStream
                          ObjectOutputStream]
           [java.net      URI
                          URL]
           [java.util     UUID
                          Date]))

(set! *warn-on-reflection* true)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Leach-Salz UUID Representation and Constituent Values (IETF RFC4122)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; The string representation of A Leach-Salz UUID has the format:
;;
;;                                          clock-seq-and-reserved
;;                                time-mid  | clock-seq-low
;;                                |         | |
;;                       6ba7b810-9dad-11d1-80b4-00c04fd430c8
;;                       |             |         |
;;                       ` time-low    |         ` node
;;                                     ` time-high-and-version
;;
;;
;; Each field is treated as integer and has its value printed as a zero-filled
;; hexadecimal digit string with the most significant digit first.
;;
;; 0                   1                   2                   3
;;  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
;; +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
;; |                        %uuid_time-low                         |
;; +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
;; |       %uuid_time-mid          |  %uuid_time-high-and-version  |
;; +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
;; |clk-seq-hi-res | clock-seq-low |         %uuid_node (0-1)      |
;; +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
;; |                         %uuid_node (2-5)                      |
;; +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
;;
;;
;;  The following table enumerates a slot/type/value correspondence:
;;
;;   SLOT       SIZE   TYPE        BYTE-ARRAY
;;  ----------------------------------------------------------------------
;;  time-low       4   ub32     [<BYTE> <BYTE> <BYTE> <BYTE>]
;;  time-mid       2   ub16     [<BYTE> <BYTE>]
;;  time-high      2   ub16     [<BYTE> <BYTE>]
;;  clock-high     1    ub8     [<BYTE>]
;;  clock-low      1    ub8     [<BYTE>]
;;  node           6   ub48     [<BYTE> <BYTE> <BYTE> <BYTE> <BYTE> <BYTE>]
;;
;; Or, as 8-bit bytes mapping into 128-bit unsigned integer values:
;;
;;  (0 7)   (8 15)  (16 23) (24 31)  ;; time-low
;;  (32 39) (40 47)                  ;; time-mid
;;  (48 55) (56 63)                  ;; time-high-and-version
;;
;;  (64 71)                          ;; clock-seq-and-reserved
;;  (72 79)                          ;; clock-seq-low
;;  (80 87)   (88 95)   (96 103)     ;;
;;  (104 111) (112 119) (120 127)    ;; node



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UUID Version and Variant
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; The variant indicates the layout of the UUID. The UUID specification
;; covers one particular variant. Other variants are reserved or exist
;; for backward compatibility reasons (e.g., for values assigned before
;; the UUID specification was produced). An example of a UUID that is a
;; different variant is the null UUID, which is a UUID that has all 128
;; bits set to zero.
;;
;; In the canonical representation, xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx,
;; the most significant bits of N indicates the variant (depending on the
;; variant one, two, or three bits are used). The variant covered by the
;; UUID specification is indicated by the two most significant bits of N
;; being 1 0 (i.e., the hexadecimal N will always be 8, 9, A, or B).
;;
;; The variant covered by the UUID specification has five versions. For this
;; variant, the four bits of M indicates the UUID version (i.e., the
;; hexadecimal M will be either 1, 2, 3, 4, or 5).
;; <http://en.wikipedia.org/wiki/Universally_unique_identifier>



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The NULL (variant 0) UUID                       [RFC4122:4.1.7 "NIL UUID"] ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; "The [null] UUID is a special form of UUID that is specified to have 
;; all 128 bits set to zero."


(def ^:const +null+           #uuid "00000000-0000-0000-0000-000000000000")



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Well-Known UUIDs                 [RFC4122:Appendix-C "SOME NAMESPACE IDs"] ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; The following UUID's are the canonical top-level namespace identifiers
;; defined in RFC4122 Appendix C.


(def ^:const +namespace-dns+  #uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
(def ^:const +namespace-url+  #uuid "6ba7b811-9dad-11d1-80b4-00c04fd430c8")
(def ^:const +namespace-oid+  #uuid "6ba7b812-9dad-11d1-80b4-00c04fd430c8")
(def ^:const +namespace-x500+ #uuid "6ba7b814-9dad-11d1-80b4-00c04fd430c8")



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Monotonic Clock (guaranteed always increasing value for time)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn monotonic-time
 "Return a monotonic timestamp (guaranteed always increasing) based on
 the number of 100-nanosecond intervals elapsed since the adoption of
 the Gregorian calendar in the West, 12:00am Friday October 15, 1582 UTC."
 []
 (clock/monotonic-time))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UUID Protocols
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defprotocol UUIDNameBytes
  "A mechanism intended for user-level extension that defines the
  decoding rules for the local-part representation of arbitrary
  Clojure / Java Objects when used for computing namespaced
  identifiers."
  
  (^bytes   as-byte-array [x]
    "Extract a byte serialization that represents the 'name' of x, 
    typically unique within a given namespace."))



(defprotocol UUIDable
  "A UUIDable object directly represents a UUID.  Examples of things which
  might be conceptually 'uuidable' include string representation of a
  UUID in canonical hex format, or an appropriate URN URI."
  
  (^UUID    as-uuid   [x]
    "Coerce the value 'x' to a UUID.")
  
  (^Boolean uuidable? [x]
    "Return 'true' if 'x' can be coerced to UUID."))



(defprotocol UUIDRfc4122
  "A protocol that abstracts an unique identifier as described by
  IETF RFC4122 <http://www.ietf.org/rfc/rfc4122.txt>. A UUID
  represents a 128-bit value, however there are variant encoding
  layouts used to assign and interpret information encoded in
  those bits.  This is a protocol for  _variant 2_ (*Leach-Salz*) 
  UUID's."
  
  (^long    hash-code       [uuid]
    "Return a suitable 64-bit hash value for `uuid`.  Extend with 
    specialized hash computation.")

  (^boolean null?           [uuid]
    "Return `true` only if `uuid` has all 128 bits set ot zero and is 
    therefore equal to the null UUID, 00000000-0000-0000-0000-000000000000.")

  (^boolean uuid?           [x]
    "Return `true` if `x` implements an RFC4122 unique identifier.")

  (^boolean uuid=           [x y]
    "Directly compare two UUID's for = relation based on the equality
    semantics defined by [RFC4122:3 RULES FOR LEXICAL EQUIVALENCE].")

  (^boolean uuid<           [x y]
    "Directly compare two UUID's for < relation based on the ordinality
    semantics defined by [RFC4122:3 RULES FOR LEXICAL EQUIVALENCE].")

  (^boolean uuid>           [x y]
    "Directly compare two UUID's for > relation based on the ordinality
    semantics defined by [RFC4122:3 RULES FOR LEXICAL EQUIVALENCE].")

  (^long    get-word-high   [uuid]
    "Return the most significant 64 bits of UUID's 128 bit value.")

  (^long    get-word-low    [uuid]
    "Return the least significant 64 bits of UUID's 128 bit value.")

  (^int     get-version     [uuid]
    "Return the version number associated with this UUID.  The version
    field contains a value which describes the nature of the UUID.  There
    are five versions of Leach-Salz UUID, plus the null UUID:

    0x0   Null
    0x1   Time based
    0x2   DCE security with POSIX UID
    0x3   Namespaced (MD5 Digest)
    0x4   Cryptographic random
    0x5   Namespaced (SHA1 Digest)

    In the canonical representation, xxxxxxxx-xxxx-Mxxx-xxxx-xxxxxxxxxxxx, 
    the four bits of M indicate the UUID version (i.e., the hexadecimal M 
    will be either 1, 2, 3, 4, or 5).")

  (^int     get-variant     [uuid]
    "Return the variant number associated with this UUID.  The variant field 
    contains a value which identifies the layout of the UUID.  The bit-layout 
    implemented by this protocol supports UUID's with a variant value of 0x2,
    which indicates Leach-Salz layout.  Defined UUID variant values are:

    0x0   Null 
    0x2   Leach-Salz
    0x6   Microsoft 
    0x7   Reserved for future assignment

    In the canonical representation, xxxxxxxx-xxxx-xxxx-Nxxx-xxxxxxxxxxxx,
    the most significant bits of N indicate the variant (depending on the
    variant one, two, or three bits are used). The variant covered by RFC4122
    is indicated by the two most significant bits of N being 1 0 (i.e., the 
    hexadecimal N will always be 8, 9, A, or B).")

  (^long    get-time-low    [uuid]
    "Return the 32 bit unsigned value that represents the `time-low` field
    of the `timestamp` associated with this UUID.")

  (^long    get-time-mid    [uuid]
    "Return the 16 bit unsigned value that represents the `time-mid` field
    of the `timestamp` assocaited with this UUID.")

  (^long    get-time-high   [uuid]
    "Return the 16 bit unsigned value that represents the `time-high` field
    of the `timestamp` multiplexed with the `version` of this UUID.")
  
  (^long    get-clk-high    [uuid]
    "Return the 8 bit unsigned value that represents the most significant
    byte of the `clk-seq` multiplexed with the `variant` of this UUID.")
  
  (^long    get-clk-low     [uuid]
    "Return the 8 bit unsigned value that represents the least significant
    byte of the `clk-seq` associated with this UUID.")

  (^short   get-clk-seq     [uuid]
    "Return the clock-sequence number associated with this UUID. For time-based
    (v1) UUID's the 'clock-sequence' value is a somewhat counter-intuitively 
    named seed-value that is used to reduce the potential that duplicate UUID's 
    might be generated under unusual situations, such as if the system hardware
    clock is set backward in time or if, despite all efforts otherwise, a 
    duplicate node-id happens to be generated. This value is initialized to 
    a random 16-bit number once per lifetime of the system.  For non-time-based
    (v3, v4, v5, squuid) UUID's, always returns `nil`.")

  (^long get-node-id        [uuid]
    "Return the 48 bit unsigned value that represents the spatially unique 
    node identifier associated with this UUID.")

  (^long get-timestamp      [uuid]
    "Return the 60 bit unsigned value that represents a temporally unique
    timestamp associated with this UUID.  For time-based (v1) UUID's the
    result encodes the number of 100 nanosecond intervals since the
    adoption of the Gregorian calendar: 12:00am Friday October 15, 1582 UTC.
    For non-time-based (v3, v4, v5, squuid) UUID's, always returns `nil`.")

  (^Date get-instant        [uuid]
    "For time-based (v1) UUID's, return a java.util.Date object that represents
    the system time at which this UUID was generated. NOTE: the returned
    value may not necessarily be temporally unique. For non-time-based 
    (v3, v4, v5, squuid) UUID's, always returns `nil`.")

  (^bytes   to-byte-array   [uuid]
    "Return an array of 16 bytes that represents `uuid` as a decomposed 
    octet serialization encoded in most-significant-byte first order.")

  (^String  to-string       [uuid]
    "Return a String object that represents `uuid` in the canonical 
    36 character hex-string format: 

        xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")

  (^String  to-hex-string   [uuid]
    "Return a String object that represents `uuid` as the 32 hexadecimal 
    characters directly encodong the UUID's 128 bit value: 

        xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")

  (^String  to-urn-string   [uuid]
    "Return a String object that represents `uuid` as a the string 
    serialization of the URN URI based on the canonical 36 character 
    hex-string representation: 

        urn:uuid:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")

  (^URI     to-uri          [uuid]
    "Return the unique URN URI associated with this UUID."))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; RFC4122 Unique Identifier extended java.util.UUID
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(extend-type UUID

  UUIDable

  (as-uuid   [u] u)
  (uuidable? [_] true)

  
  UUIDRfc4122

  (uuid? [_] true)

  (uuid= [x ^UUID y]
    (.equals x y))

  (uuid< [x ^UUID y]
    (let [xh (.getMostSignificantBits x)
          yh (.getMostSignificantBits y)]
      (or (< xh yh)
        (and (= xh yh) (< (.getLeastSignificantBits x)
                         (.getLeastSignificantBits y))))))

  (uuid> [x ^UUID y]
    (let [xh (.getMostSignificantBits x)
          yh (.getMostSignificantBits y)]
      (or (> xh yh)
        (and (= xh yh) (> (.getLeastSignificantBits x)
                         (.getLeastSignificantBits y))))))

  (get-word-high [uuid]
    (.getMostSignificantBits uuid))

  (get-word-low [uuid]
    (.getLeastSignificantBits uuid))

  (null? [uuid]
    (= 0 (.getMostSignificantBits uuid) (.getLeastSignificantBits uuid)))

  (to-byte-array [uuid]
    (let [arr (byte-array 16)]
      (bitmop/long->bytes (.getMostSignificantBits  uuid) arr 0)
      (bitmop/long->bytes (.getLeastSignificantBits uuid) arr 8)
      arr))

  (hash-code [uuid]
    (long (.hashCode uuid)))
  
  (get-version [uuid]
    (.version uuid))

  (to-string [uuid]
    (.toString uuid))

  (to-urn-string [uuid]
    (str "urn:uuid:" (to-string uuid)))

  (to-uri [uuid]
    (URI/create (to-urn-string uuid)))

  (get-time-low [uuid]
    (bitmop/ldb (bitmop/mask 32 0)
      (bit-shift-right (.getMostSignificantBits uuid) 32)))

  (get-time-mid [uuid]
    (bitmop/ldb (bitmop/mask 16 16)
      (.getMostSignificantBits uuid)))

  (get-time-high [uuid]
    (bitmop/ldb (bitmop/mask 16 0)
      (.getMostSignificantBits uuid)))
  
  (get-clk-low [uuid]
    (bitmop/ldb (bitmop/mask 8 0)
      (bit-shift-right (.getLeastSignificantBits uuid) 56)))

  (get-clk-high [uuid]
    (bitmop/ldb (bitmop/mask 8 48)
      (.getLeastSignificantBits uuid)))

  (get-node-id [uuid]
    (bitmop/ldb (bitmop/mask 48 0)
      (.getLeastSignificantBits uuid)))

  (get-timestamp [uuid]
    (when (= 1 (get-version uuid))
      (.timestamp uuid)))

  
  UUIDNameBytes
  
  (as-byte-array [this]
    (to-byte-array this)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; V0 UUID Constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^UUID null
  "Generates the v0 (null) UUID, 00000000-0000-0000-0000-000000000000."
  []
  +null+)

(defn ^UUID v0
  "Generates the v0 (null) UUID, 00000000-0000-0000-0000-000000000000."
  []
  +null+)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; V1 UUID Constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Concatenate the UUID version with the MAC address of the computer that is
;; generating the UUID, and with a monotonic timestamp based on the number
;; of 100-nanosecond intervals since the adoption of the Gregorian calendar
;; in the West, 12:00am Friday October 15, 1582 UTC.

(defn ^UUID v1
  "Generate a v1 (time-based) unique identifier, guaranteed to be unique
  and thread-safe regardless of clock precision or degree of concurrency.
  Creation of v1 UUID's does not require any call to a cryptographic 
  generator and can be accomplished much more efficiently than v3, v4, v5,
  or squuid's.  A v1 UUID reveals both the identity of the computer that 
  generated the UUID and the time at which it did so.  Its uniqueness across 
  computers is guaranteed as long as MAC addresses are not duplicated."
  []
  (let [ts        (clock/monotonic-time)
        time-low  (bitmop/ldb (bitmop/mask 32  0)  ts)
        time-mid  (bitmop/ldb (bitmop/mask 16 32)  ts)
        time-high (bitmop/dpb (bitmop/mask 4  12)
                    (bitmop/ldb (bitmop/mask 12 48) ts) 0x1)
        msb       (bit-or time-high
                   (bit-shift-left time-low 32)
                   (bit-shift-left time-mid 16))]
    (UUID. msb node/+v1-lsb+)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; V4 (random) UUID Constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^UUID v4
  "Generate a v4 (random) UUID.  Uses default JVM implementation.  If two
  arguments, lsb and msb (both long) are provided, then construct a valid,
  properly formatted v4 UUID based on those values.  So, for example the
  following UUID, created from all zero bits, is indeed distinct from the
  null UUID:

      (v4)
       => #uuid \"dcf0035f-ea29-4d1c-b52e-4ea499c6323e\"

      (v4 0 0)
       => #uuid \"00000000-0000-4000-8000-000000000000\"

      (null)
       => #uuid \"00000000-0000-0000-0000-000000000000\""
  ([]
    (UUID/randomUUID))
  ([^long msb ^long lsb]
    (UUID.
      (bitmop/dpb (bitmop/mask 4 12) msb 0x4)
      (bitmop/dpb (bitmop/mask 2 62) lsb 0x2))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SQUUID (sequential) UUID Constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^UUID squuid
  "Generate a SQUUID (sequential, random) unique identifier.  SQUUID's
  are a nonstandard variation on v4 (random) UUIDs that have the
  desirable property that they increase sequentially over time as well
  as encode retrievably the posix time at which they were generated.
  Splits and reassembles a v4 UUID to merge current POSIX
  time (seconds since 12:00am January 1, 1970 UTC) with the most
  significant 32 bits of the UUID."
  []
  (let [uuid (v4)
        secs (clock/posix-time)
        lsb  (get-word-low  uuid)
        msb  (get-word-high uuid)
        timed-msb (bit-or (bit-shift-left secs 32)
                    (bit-and +ub32-mask+ msb))]
    (UUID. timed-msb lsb)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; "Local-Part" Representation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; The following represent a default set of local-part encoding rules.  As a
;; default, a plain byte-array will be passed through unchanged and a
;; generic java.lang.Object is represented by the bytes of its serialization.
;; Strings are represented using UTF8 encoding.  URL's are digested as the
;; UTF bytes of their string representation.

(def ^:private ByteArray (class (byte-array 0)))

(extend-protocol UUIDNameBytes

  java.lang.Object
  (^bytes as-byte-array [this]
    (if (instance? ByteArray this)
      this
      (let [baos (ByteArrayOutputStream.)
            oos  (ObjectOutputStream. baos)]
        (.writeObject oos this)
        (.close oos)
        (.toByteArray baos))))

  java.lang.String
  (^bytes as-byte-array [this]
    (compile-if (java6?)
      (.getBytes this)
      (.getBytes this java.nio.charset.StandardCharsets/UTF_8)))

  java.net.URL
  (^bytes as-byte-array [this]
    (as-byte-array (.toString this))))
 


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Digest Instance
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ^MessageDigest make-digest [^String designator]
  (MessageDigest/getInstance designator))

(defn- ^bytes digest-bytes  [^String kind ^bytes ns-bytes ^bytes local-bytes]
  (let [^MessageDigest m (make-digest kind)]    
    (.update m ns-bytes)
    (.digest m local-bytes)))

(defn- ^UUID build-digested-uuid [^long version ^bytes arr]
  {:pre [(or (= version 3) (= version 5))]}   
  (let [msb (bitmop/bytes->long arr 0)
        lsb (bitmop/bytes->long arr 8)]
    (UUID.
     (bitmop/dpb (bitmop/mask 4 12) msb version)
     (bitmop/dpb (bitmop/mask 2 62) lsb 0x2))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Namespaced UUIDs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^UUID v3
  "Generate a v3 (name based, MD5 hash) UUID. 'context' must be UUIDable."
  [context local-part]
  (build-digested-uuid 3
    (digest-bytes +md5+
      (to-byte-array (as-uuid context))
      (as-byte-array local-part))))


(defn ^UUID v5
  "Generate a v5 (name based, SHA1 hash) UUID. 'context' must be UUIDable."
  [context local-part]
  (build-digested-uuid 5
    (digest-bytes +sha1+
      (to-byte-array (as-uuid context))
      (as-byte-array local-part))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Predicates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; NOTE: (not (nil? ...)) avoids returning some generalized boolean value that
;; might be counterintuitive to the user, since these functions are exposed
;; as user api.


(defn uuid-string? [str]
  (not (nil? (re-matches uuid-regex str))))

(defn uuid-urn-string? [str]
  (not (nil? (re-matches urn-regex str))))

(defn uuid-vec? [v]
  (and (= (count v) 16)
    (every? #(and (integer? %) (>= -128  %) (<=  127  %)) v)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UUID Polymorphism
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- str->uuid [s]
  (cond
    (uuid-string?     s) (UUID/fromString s)
    (uuid-urn-string? s) (UUID/fromString (subs s 9))
    :else                (exception "invalid UUID")))

(extend-protocol UUIDRfc4122
  Object
  (uuid? [x] false))

(extend-protocol UUIDable
  String 
  (uuidable? [s]
    (or
     (uuid-string?     s)
     (uuid-urn-string? s)))
  (as-uuid [s]
    (str->uuid s))

  URI
  (uuidable? [u]
    (uuid-urn-string? (str u)))
  (as-uuid [u]
    (str->uuid (str u)))
  
  Object
  (uuidable? [_]
    false)
  (as-uuid [x]
    (exception IllegalArgumentException x "Cannot be coerced to UUID.")))

 
(set! *warn-on-reflection* false)
