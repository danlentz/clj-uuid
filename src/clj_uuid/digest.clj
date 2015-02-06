(ns clj-uuid.digest
  (:use [clojure.core])
  (:use [clj-uuid.constants])
  (:use [clj-uuid.bitmop]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Digest Instance
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def +md5+  "MD5")
(def +sha1+ "SHA1")

(defn make-digest [designator]
  (java.security.MessageDigest/getInstance designator))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SHA1 Digest
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti sha1 class)

(defmethod sha1 String [s]
  (sbvec
    (seq
      (-> (make-digest +sha1+)
        (.digest (.getBytes s))))))

(defmethod sha1 clojure.core.Vec [coll]
  (sbvec
    (seq
      (-> (make-digest +sha1+)
        (.digest (byte-array (map sb8 coll)))))))

(defmethod sha1 clojure.lang.PersistentVector [coll]
  (sha1 (sbvec coll)))

(defmethod sha1 Object [o]
  (sha1 (.toString o)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MD5 Digest
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti md5 class)

(defmethod md5 String [s]
  (sbvec
    (seq
      (-> (make-digest +md5+)
          (.digest (.getBytes s))))))

(defmethod md5 clojure.core.Vec [coll]
  (sbvec
    (seq 
      (-> (make-digest +md5+)
          (.digest (byte-array (map sb8 coll)))))))

(defmethod md5 clojure.lang.PersistentVector [coll]
  (md5 (sbvec coll)))

(defmethod md5 Object [o]
  (md5 (.toString o)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Digest Namespaced Identifiers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- digest-uuid-bytes [digest uuid-bytes namestring]
  (apply sbvector
    (subvec (digest (sbvec (concat uuid-bytes (seq (.getBytes namestring)))))
      0 16)))
