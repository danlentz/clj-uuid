(ns clj-uuid.digest
  (:use [clojure.core])
  (:use [clojure.pprint])
  (:use [clojure.repl :only [doc find-doc apropos]])
  (:use [clj-uuid.constants])
  (:use [clj-uuid.bitmop]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Digest Instance
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce +md5+  "MD5")
(defonce +sha1+ "SHA1")

(defn make-digest [designator]
  (java.security.MessageDigest/getInstance designator))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SHA1 Digest
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti sha1 type)

(defmethod sha1 String [s]
  (sbvec
    (seq
      (-> (make-digest +sha1+)
        (.digest (.getBytes s))))))

(=    (sha1 "")    (sha1 ""))
(=    (sha1 "x")   (sha1 "x"))
(=    (sha1 "xyz") (sha1 "xyz"))
(not= (sha1 "xyz") (sha1 "x"))

(defmethod sha1 clojure.core.Vec [coll]
  (sbvec
    (seq
      (-> (make-digest +sha1+)
        (.digest (byte-array (map sb8 coll)))))))

(defmethod sha1 clojure.lang.PersistentVector [coll]
  (sha1 (sbvec coll)))

(=    (sha1 (sha1 "x")) (sha1 (sha1 "x")))
(=    (sha1 (sha1 "xyz")) (sha1 (sha1 "xyz")))
(not= (sha1 (sha1 "x")) (sha1 (sha1 "xyz")))

(defmethod sha1 Object [o]
  (sha1 (.toString o)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MD5 Digest
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti md5 type)

(defmethod md5 String [s]
  (sbvec
    (seq
      (-> (make-digest +md5+)
          (.digest (.getBytes s))))))

(= (md5 "")    (md5 ""))
(= (md5 "x")   (md5 "x"))
(= (md5 "xyz") (md5 "xyz"))
(not= (md5 "xyz") (md5 "x"))


(defmethod md5 clojure.core.Vec [coll]
  (sbvec
    (seq 
      (-> (make-digest +md5+)
          (.digest (byte-array (map sb8 coll)))))))

(defmethod md5 clojure.lang.PersistentVector [coll]
  (md5 (sbvec coll)))


(=    (md5 (md5 "x"))   (md5 (md5 "x")))
(=    (md5 (md5 "xyz")) (md5 (md5 "xyz")))
(not= (md5 (md5 "x"))   (md5 (md5 "xyz")))

(defmethod md5 Object [o]
  (md5 (.toString o)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Digest Namespaced Identifiers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn digest-uuid-bytes [digest uuid-bytes namestring]
  (apply sbvector
         (subvec (digest (sbvec (concat uuid-bytes
                                        (seq (.getBytes namestring)))))
                 0 16)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MD5/SHA1 Performance Experimentation / Comparative Timings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (comment
;;   (do
;;     (println "====")
;;     (time
;;       (dotimes [i 10000]
;;         (sha1 (hex (rand-int 0x7fffffff)))))
;;     (time
;;       (dotimes [i 10000]
;;         (md5 (hex (rand-int 0x7fffffff)))))
;;     (println )))
