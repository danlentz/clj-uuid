(ns clj-uuid
  (:refer-clojure :exclude [==])
  (:require [clj-uuid
             [constants :refer :all]
             [util      :refer :all]
             [bitmop    :as bitmop]
             [digest    :as digest]
             [clock     :as clock]
             [node      :as node]])
  (:import [java.net  URI URL]
           [java.util UUID]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Well-Known UUIDs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:const +namespace-dns+  #uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
(def ^:const +namespace-url+  #uuid "6ba7b811-9dad-11d1-80b4-00c04fd430c8")
(def ^:const +namespace-oid+  #uuid "6ba7b812-9dad-11d1-80b4-00c04fd430c8")
(def ^:const +namespace-x500+ #uuid "6ba7b814-9dad-11d1-80b4-00c04fd430c8")
(def ^:const +null+           #uuid "00000000-0000-0000-0000-000000000000")


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UniqueIdentifier Protocol
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol UniqueIdentifier
  (null?           [uuid])
  (uuid?           [uuid])
  (uuid            [uuid])
  (uuid=           [x y])
  (get-word-high   [uuid])
  (get-word-low    [uuid])
  (hash-code       [uuid])
  (get-version     [uuid])
  (to-string       [uuid])
  (to-hex-string   [uuid])
  (to-urn-string   [uuid])
  (to-octet-vector [uuid])
  (to-byte-vector  [uuid])
  (to-uri          [uuid])
  (get-time-low    [uuid])
  (get-time-mid    [uuid])
  (get-time-high   [uuid])
  (get-clk-low     [uuid])
  (get-clk-high    [uuid])
  (get-node-id     [uuid])
  (get-timestamp   [uuid]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UniqueIdentifier extended UUID
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-type UUID
  UniqueIdentifier
  (uuid [u] u)
  (uuid? [_] true)
  (uuid= [x y]
    (.equals x y))
  (get-word-high [uuid]
    (.getMostSignificantBits uuid))
  (get-word-low [uuid]
    (.getLeastSignificantBits uuid))
  (null? [uuid]
    (= 0 (get-word-low uuid)))
  (to-byte-vector [uuid]
    (bitmop/sbvec (concat
                    (bitmop/sbvec (get-word-high uuid))
                    (bitmop/sbvec (get-word-low uuid)))))
  (to-octet-vector [uuid]
    (bitmop/ubvec (concat
                    (bitmop/ubvec (get-word-high uuid))
                    (bitmop/ubvec (get-word-low uuid)))))
  (hash-code [uuid]
    (.hashCode uuid))
  (get-version [uuid]
    (.version uuid))
  (to-string [uuid]
    (.toString uuid))
  (to-hex-string [uuid]
    (str
      (bitmop/hex (get-word-high uuid))
      (bitmop/hex (get-word-low uuid))))
  (to-urn-string [uuid]
    (str "urn:uuid:" (to-string uuid)))
  (to-uri [uuid]
    (URI/create (to-urn-string uuid)))
  (get-time-low [uuid]
    (bitmop/ldb (bitmop/mask 32 0) (bit-shift-right (get-word-high uuid) 32)))
  (get-time-mid [uuid]
    (bitmop/ldb (bitmop/mask 16 16) (get-word-high uuid)))
  (get-time-high [uuid]
    (bitmop/ldb (bitmop/mask 16 0) (get-word-high uuid)))
  (get-clk-low [uuid]
    (bitmop/ldb (bitmop/mask 8 0) (bit-shift-right (get-word-low uuid) 56)))
  (get-clk-high [uuid]
    (bitmop/ldb (bitmop/mask 8 48) (get-word-low uuid)))
  (get-node-id [uuid]
    (bitmop/ldb (bitmop/mask 48 0) (get-word-low uuid)))
  (get-timestamp [uuid]
    (when (= 1 (get-version uuid))
      (.timestamp uuid))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; General Representation Of UUID Constituent Values
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; The string representation of A UUID has the format:
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
;;  time-low       4   ub32     #(<BYTE> <BYTE> <BYTE> <BYTE>)
;;  time-mid       2   ub16     #(<BYTE> <BYTE>)
;;  time-high      2   ub16     #(<BYTE> <BYTE>)
;;  clk-high       1    ub8     #(<BYTE>)
;;  clock-low      1    ub8     #(<BYTE>)
;;  node           6   ub48     #(<BYTE> <BYTE> <BYTE> <BYTE> <BYTE> <BYTE>)
;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 8-bit Bytes mapping into 128-bit unsigned integer values
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;;  ((0 7)   (8 15)  (16 23) (24 31) ;; time-low
;;  (32 39) (40 47)                  ;; time-mid
;;  (48 55) (56 63)                  ;; time-high-and-version
;;
;;  (64 71)                          ;; clock-seq-and-reserved
;;  (72 79)                          ;; clock-seq-low
;;  (80 87)   (88 95)   (96 103)     ;;
;;  (104 111) (112 119) (120 127))   ;; node


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UUID Constituent Data Map
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NOTE: at least in its present form this is probably too fragile and
;; not all that useful anyway.  So most likely it should go away, I think.
;; It does come in handy for use in the repl during development sometimes...
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-uuid-data [uuid]
  (let [fns '[get-version get-time-low get-time-mid get-time-high
              get-clk-low get-clk-high get-node-id]]
    (zipmap
     (map (comp keyword name) fns)
     (map #((ns-resolve *ns* %) uuid) fns))))

;; (=
;;   (get-uuid-data +null+)
;;   {:get-node-id 0,
;;     :get-clk-high 0,
;;     :get-clk-low 0,
;;     :get-time-high 0,
;;     :get-time-mid 0,
;;     :get-time-low 0
;;    :get-version 0})

;; (=
;;   (get-uuid-data #uuid "0edf17a3-436d-4354-8969-79033e1a0607")
;;   {:get-node-id 133054833755655,
;;     :get-clk-high 105,
;;     :get-clk-low 137,
;;     :get-time-high 17236,
;;     :get-time-mid 17261,
;;     :get-time-low 249501603
;;     :get-version 4})

;; (=
;;   (get-uuid-data +namespace-oid+)
;;   {:get-node-id 825973027016,
;;     :get-clk-high 180,
;;     :get-clk-low 128,
;;     :get-time-high 4561,
;;     :get-time-mid 40365,
;;     :get-time-low 1806153746
;;     :get-version 1})

;; (=
;;   (get-uuid-data +namespace-dns+)
;;   (get-uuid-data +namespace-dns+))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; V0 UUID Constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn null ^UUID []
  +null+)

(defn v0 ^UUID []
  +null+)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; V1 UUID Constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn v1
  "Generate a v1 (time-based) unique identifier."
  ^UUID []
  (let [ts (clock/monotonic-time)
        time-low  (bitmop/ldb (bitmop/mask 32  0) ts)
        time-mid  (bitmop/ldb (bitmop/mask 16 32) ts)
        time-high (bitmop/dpb (bitmop/mask 4 12)
                    (bitmop/ldb (bitmop/mask 12 48) ts) 0x1)
        msb       (bit-or
                   (bit-shift-left time-low 32)
                   (bit-shift-left time-mid 16)
                   time-high)
        clk-high  (bitmop/dpb (bitmop/mask 2 6)
                    (bitmop/ldb
                      (bitmop/mask 6 8)
                      clock/+clock-seq+) 0x2)
        clk-low   (bitmop/ldb (bitmop/mask 8 0) clock/+clock-seq+)
        lsb       (bitmop/assemble-bytes
                    (concat [clk-high clk-low] node/+node-id+))]
    (UUID. msb lsb)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; V4 (random) UUID Constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn v4 ^UUID []
  (UUID/randomUUID))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SQUUID (sequential) UUID Constructor
;; 
;; splits and reassembles a random UUID using bit-or to merge the current time
;; with the most significant 32 bits of the UUID.  The two halves of the UUID
;; are reassembled using the UUID. constructor, yielding UUIDs that increase
;; sequentially over time.  credit: cognitect/datomic; clojure-cookbook
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: see about a version that eliminates call to random number generator

(defn squuid ^UUID []
  (let [uuid (UUID/randomUUID)
        time (System/currentTimeMillis)
        secs (quot time 1000)
        lsb (.getLeastSignificantBits uuid)
        msb (.getMostSignificantBits uuid)
        timed-msb (bit-or (bit-shift-left secs 32)
                    (bit-and 0x00000000ffffffff msb))]
    (UUID. timed-msb lsb)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Namespaced UUIDs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fmt-digested-uuid [version bytes]
  (assert (or (= version 3) (= version 5)))
  (let [msb (bitmop/assemble-bytes (take 8 bytes))
        lsb (bitmop/assemble-bytes (drop 8 bytes))]
    (UUID.
     (bitmop/dpb (bitmop/mask 4 12) msb version)
     (bitmop/dpb (bitmop/mask 2 62) lsb 0x2))))

(defn v5 ^UUID [context namestring]
  (fmt-digested-uuid 5
    (digest/digest-uuid-bytes digest/sha1 (to-byte-vector context) namestring)))

(defn v3 ^UUID [context namestring]
  (fmt-digested-uuid 3
    (digest/digest-uuid-bytes digest/md5 (to-byte-vector context) namestring)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Predicates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn uuid-string? [str]
  (not (nil? (re-matches &uuid-string str))))

(defn uuid-hex-string? [str]
  (not (nil? (re-matches &uuid-hex-string str))))

(defn uuid-urn-string? [str]
  (not (nil? (re-matches &uuid-urn-string str))))

(defn uuid-vec? [v]
  (and
   (= (count v) 16)
   (every? #(and
             (integer? %)
             (>= -128  %)
             (<=  127  %))
           v)))

(defn str->uuid [s]
  (cond
    (uuid-string?     s) (UUID/fromString s)
    (uuid-hex-string? s) (UUID. (bitmop/unhex (subs s 0 16)) (bitmop/unhex (subs s 16 32)))
    (uuid-urn-string? s) (UUID/fromString (subs s 9))
    :else                (exception "invalid UUID")))

(extend-protocol UniqueIdentifier
  String
  (uuid? [s]
    (or
     (uuid-string?     s)
     (uuid-hex-string? s)
     (uuid-urn-string? s)))
  (uuid [s] (str->uuid s))
  clojure.lang.PersistentVector
  (uuid? [v] (uuid-vec? v))
  clojure.core.Vec
  (uuid? [v] (uuid-vec? v))
  URI
  (uuid? [u]
    (uuid-urn-string? (str u)))
  (uuid [u] (str->uuid (str u)))
  Object
  (uuid? [_] false))
