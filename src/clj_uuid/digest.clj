(ns clj-uuid.digest
  (:use [clojure.core])
  (:use [clj-uuid.constants])
  (:use [clj-uuid.bitmop])
  (:require [clj-uuid.util :as util])
  (:import [java.net URL])
  (:import [java.security MessageDigest])
  (:import [java.io ByteArrayOutputStream ObjectOutputStream]))

(set! *warn-on-reflection* true)


(def ByteArray (class (byte-array 0)))

(defprotocol UUIDNameBytes
  (^bytes as-byte-array [x] "unique byte serialization"))

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
    (util/compile-if (neg? (compare (System/getProperty "java.version") "1.7"))
      (.getBytes this)
      (.getBytes this java.nio.charset.StandardCharsets/UTF_8)))

  java.net.URL
  (^bytes as-byte-array [this]
    (as-byte-array (.toString this))))
 


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Digest Instance
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def +md5+  "MD5")
(def +sha1+ "SHA1")


(defn make-digest [designator]
  (MessageDigest/getInstance designator))

(defn digest-bytes  [kind ^bytes ns-bytes ^bytes local-bytes]
  (let [^MessageDigest m (make-digest kind)]    
    (.update m ns-bytes)
    (.digest m local-bytes)))



(set! *warn-on-reflection* false)









