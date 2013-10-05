(ns clj-uuid.bitmop
  (:use [clojure.core])
  (:use [clojure.pprint])
  (:use [clojure.repl :only [doc find-doc apropos]])
  (:use [clj-uuid.constants])
  (:use [clj-uuid.bitmop :as bitmop])
  (:use [clj-uuid.digest :as digest])
  (:use [clj-uuid.clock  :as clock])
  (:import (java.net  URI URL))
  (:import (java.util UUID)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UniqueIdentifier Protocol
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol UniqueIdentifier
  (null?           [uuid])
  (uuid=           [x y]) 
  (get-word-high       [uuid])
  (get-word-low        [uuid])
  (hash-code       [uuid])
  (get-version         [uuid])
  (to-string       [uuid])
  (to-hex-string   [uuid])
  (to-urn-string   [uuid])
  (to-octet-vector [uuid])
  (get-time-low        [uuid])
  (get-time-mid        [uuid])
  (get-time-high       [uuid])
  (get-clk-low         [uuid])
  (get-clk-high        [uuid])
  (get-node-id            [uuid])
  (get-timestamp       [uuid])
  (get-namespace-bytes [uuid]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UniqueIdentifier extended UUID
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-type UUID UniqueIdentifier
  (uuid= [x y]
    (.equals x y))
  (get-word-high [uuid]
    (.getMostSignificantBits uuid))
  (get-word-low [uuid]
    (.getLeastSignificantBits uuid))  
  (null? [uuid]
    (= 0 (get-word-low uuid)))
  (to-octet-vector [uuid]
    (bitmop/words (get-word-high uuid) (get-word-low uuid)))
  (hash-code [uuid]
    (.hashCode uuid))
  (get-version [uuid]
    (.version uuid))
  (to-string [uuid]
    (.toString uuid))
  (to-hex-string [uuid]
    (bitmop/hex (bitmop/to-octet-vector uuid)))
  (to-urn-string [uuid]
    (str "urn:uuid:" (to-string uuid)))
  (get-time-low [uuid]    
    (bitmop/svec (take 4 (bitmop/word (get-word-high uuid)))))
  (get-time-mid [uuid]
    (bitmop/svec (take 2 (drop 4 (bitmop/word (get-word-high uuid))))))
  (get-time-high [uuid]
    (bitmop/svec (take 2 (drop 6 (bitmop/word (get-word-high uuid))))))
  (get-clk-low [uuid]
    (bitmop/svec (take 1 (bitmop/word (get-word-low uuid)))))
  (get-clk-high [uuid]
    (bitmop/svec (take 1 (rest (bitmop/word (get-word-low uuid))))))    
  (get-node-id [uuid]
    (bitmop/svec (take 6 (drop 2 (bitmop/word (get-word-low uuid))))))
  (get-timestamp [uuid]
    (when (= 1 (get-version uuid))
      (.timestamp uuid)))
  (get-namespace-bytes [uuid]
    :nyi))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; V0 UUID Constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-null-uuid []
  +null+)

(defn make-v0-uuid []
  +null+)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; V4 UUID Constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  
(defn make-v4-uuid []
  (UUID/randomUUID))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; V1 UUID Constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-v1-uuid []
  (let [ts (clock/make-timestamp) v (bitmop/octets ts)]
    (UUID/fromString
      (str 
        (bitmop/hex (drop 4 v))
        "-"
        (bitmop/hex  (take 2 (drop 2 v)))
        "-"
        (bitmop/hex (bitmop/svector (bit-or 0x10
                                      (bitmop/ub4 (first v))) (second v)))
        "-"
        (bitmop/hex (bitmop/svector (bit-or 64
                                      (bit-and 63
                                        (bit-shift-right @clock/clock-seq 8)))))
        (bitmop/hex (bitmop/svector (bitmop/ub8 @clock/clock-seq)))
        "-"
        (bitmop/hex +node-id+)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Namespaced UUIDs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- format-digested-uuid [version octv]
  (assert (or (= version 3) (= version 5)))
  (str
    (bitmop/hex (subvec octv 0 4))
    "-"
    (bitmop/hex (subvec octv 4 6))
    "-"
    (bitmop/hex (svector
                  (bit-or
                    (bit-shift-left (bitmop/ub4 version) 4)
                    (bitmop/ub4 (nth octv 6)))
                  (nth octv 7)))
    "-"
    (bitmop/hex (subvec octv 8 10))
    "-"
    (bitmop/hex (subvec octv 10 16))))

(defn make-v3-uuid [context namestring]
  (assert (= (type context) UUID))
  (->> 
    (digest/digest-uuid digest/md5 context namestring)
    (format-digested-uuid 3)
    UUID/fromString))

(defn make-v5-uuid [context namestring]
  (assert (= (type context) UUID))
  (->> 
    (digest/digest-uuid digest/sha1 context namestring)
    (format-digested-uuid 5)
    UUID/fromString))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Predicates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn uuid? [thing]
  (= (type thing) UUID))

(defn uuid-string? [str]
  (not (nil? (re-matches &uuid-string str))))

(defn uuid-hex-string? [str]
  (not (nil? (re-matches &uuid-hex-string str))))

(defn uuid-urn-string? [str]
  (not (nil? (re-matches &uuid-urn-string str))))

(defn uuid-octet-vector? [v]
  (and
    (= 16   (count v))
    (every? #(=  %  java.lang.Short) (map type v))
    (every? #(<= %  0xff) v)))


