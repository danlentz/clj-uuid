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
;; SHA1 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti sha1 type)

(defmethod sha1 String [s]
  (svec
    (map ub8  (-> (make-digest +sha1+)
                (.digest (.getBytes s))))))

(defmethod sha1 Object [o]
  (sha1 (.toString o)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MD5
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti md5 type)

(defmethod md5 String [s]
  (svec
    (map ub8  (-> (make-digest +md5+)
                (.digest (.getBytes s))))))

(defmethod md5 Object [o]
  (md5 (.toString o)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MD5/SHA1 Performance Metrics / Comparative Timings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (do
    (println "====")
    (time
      (dotimes [i 10000]
        (sha1 (hex (rand-int 0x7fffffff)))))
    (time
      (dotimes [i 10000]
        (md5 (hex (rand-int 0x7fffffff)))))
    (println )))
