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
    (map sb8
      (-> (make-digest +sha1+)
        (.digest (.getBytes s))))))

(=    (sha1 "")    (sha1 ""))
(=    (sha1 "x")   (sha1 "x"))
(=    (sha1 "xyz") (sha1 "xyz"))
(not= (sha1 "xyz") (sha1 "x"))


(defmethod sha1 clojure.core.Vec [coll]
  (sbvec
    (map sb8 
      (-> (make-digest +sha1+)
        (.digest (byte-array (map sb8 coll)))))))

                   ;; (apply concat (map #(cond
                   ;;                                 (coll? %) (.getBytes %)
                   ;;                                 :else [(sb8 %)])
                   ;;                           coll))))))))

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
    (map sb8
      (-> (make-digest +md5+)
        (.digest (.getBytes s))))))

(= (md5 "")    (md5 ""))
(= (md5 "x")   (md5 "x"))
(= (md5 "xyz") (md5 "xyz"))
(not= (md5 "xyz") (md5 "x"))


(defmethod md5 clojure.core.Vec [coll]
  (sbvec
    (map sb8 
      (-> (make-digest +md5+)
        (.digest (byte-array (map sb8 coll)))))))

(=    (md5 (md5 "x"))   (md5 (md5 "x")))
(=    (md5 (md5 "xyz")) (md5 (md5 "xyz")))
(not= (md5 (md5 "x"))   (md5 (md5 "xyz")))

(defmethod md5 Object [o]
  (md5 (.toString o)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Digest Namespaced Identifiers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn digest-uuid-str [digest uuid & namestring-parts]
;;  (subvec (digest (vec (cons (.toString uuid) namestring-parts))) 0 16))

(defn digest-uuid-bytes [digest uuid-bytes namestring]
  (apply sbvector
    (subvec (digest
              (concat uuid-bytes
                (seq (.getBytes namestring))))
      0 16)))

(= (digest-uuid-bytes sha1 (concat (sbvec -1) (sbvec -1)) "")
  (sbvec [-73 46 20 91 7 104 59 84 103 -52 -118 -53 -39 -36 91 -37]))
(= (digest-uuid-bytes sha1 (concat (sbvec -1111) (sbvec -1111)) "x")
  (sbvec [51 88 53 -106 45 19 -86 -76 -104 -125 -70 103 15 12 26 -101]))
(= (digest-uuid-bytes md5 (concat (sbvec -1) (sbvec -1)) "")
  (sbvec [-101 -52 -64 -103 -26 -111 116 54 -45 -42 121 26 -110 -52 112 28]))
(= (digest-uuid-bytes md5 (concat (sbvec -1111) (sbvec -1111)) "x")
  (sbvec [-31 -19 -78 58 -47 35 100 -64 43 -63 -44 127 -113 24 37 -3]))



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
