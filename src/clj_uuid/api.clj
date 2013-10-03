(ns clj-uuid.bitmop
  (:use [clojure.core])
  (:use [clojure.pprint])
  (:use [clojure.repl :only [doc find-doc apropos]])
  (:use [clj-uuid.constants])
  (:use [clj-uuid.bitmop])
  (:use [clj-uuid.digest])
  (:import (java.net  URI URL))
  (:import (java.util UUID)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UniqueIdentifier Protocol
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol UniqueIdentifier
  (null?           [uuid])
  (word-high       [uuid])
  (word-low        [uuid])
  (hash-code       [uuid])
  (version         [uuid])
  (to-string       [uuid])
  (to-hex-string   [uuid])
  (to-urn-string   [uuid])
  (to-octet-vector [uuid])
  (time-low        [uuid])
  (time-mid        [uuid])
  (time-high       [uuid])
  (clk-low         [uuid])
  (clk-high        [uuid])
  (node            [uuid])
  (namespace-bytes [uuid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UniqueIdentifier extended UUID
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-type UUID UniqueIdentifier
  (word-high [uuid]
    (.getMostSignificantBits uuid))
  (word-low [uuid]
    (.getLeastSignificantBits uuid))  
  (null? [uuid]
    (= 0 (word-low uuid)))
  (to-octet-vector [uuid]
    (words (word-high uuid) (word-low uuid)))
  (hash-code [uuid]
    (.hashCode uuid))
  (version [uuid]
    (.version uuid))
  (to-string [uuid]
    (.toString uuid))
  (to-hex-string [uuid]
    (hex (to-octet-vector uuid)))
  (to-urn-string [uuid]
    (str "urn:uuid:" (to-string uuid)))
  (time-low [uuid]    
    (svec (take 4 (word (word-high uuid)))))
  (time-mid [uuid]
    (svec (take 2 (drop 4 (word (word-high uuid))))))
  (time-high [uuid]
    (svec (take 2 (drop 6 (word (word-high uuid))))))
  (clk-low [uuid]
    (svec (take 1 (word (word-low uuid)))))
  (clk-high [uuid]
    (svec (take 1 (rest (word (word-low uuid))))))    
  (node [uuid]
    (svec (take 6 (drop 2 (word (word-low uuid))))))
  (namespace-bytes [uuid]
    :nyi))


;; v0 uuid protocol test
(let [tmpid +null+]
  (= (word-high tmpid)       0)
  (= (word-low tmpid)        0)
  (= (null? tmpid)           true)
  (= (to-octet-vector tmpid) [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0])
  (= (hash-code tmpid)       0)
  (= (version tmpid)         0)
  (= (to-string tmpid)       "00000000-0000-0000-0000-000000000000")
  (= (to-hex-string tmpid)   "00000000000000000000000000000000")   
  (= (to-urn-string tmpid)   "urn:uuid:00000000-0000-0000-0000-000000000000")
  (= (time-low tmpid)       [0 0 0 0])
  (= (time-mid tmpid)       [0 0])
  (= (time-high tmpid)      [0 0])
  (= (clk-low tmpid)        [0])
  (= (clk-high tmpid)       [0])
  (= (node tmpid)           [0 0 0 0 0 0])
  )

;; v1 uuid protocol test
(let [tmpid +namespace-x500+]
  (= (word-high tmpid)       7757371281853190609)
  (= (word-low tmpid)        -9172705715073830712)
  (= (null? tmpid)           false)
  (= (to-octet-vector tmpid) [107 167 184 20 157 173 17 209 128 180 0 192 79 212 48 200])
  (= (hash-code tmpid)       963287501)
  (= (version tmpid)         1)
  (= (to-string tmpid)       "6ba7b814-9dad-11d1-80b4-00c04fd430c8")
  (= (to-hex-string tmpid)   "6BA7B8149DAD11D180B400C04FD430C8")
  (= (to-urn-string tmpid)   "urn:uuid:6ba7b814-9dad-11d1-80b4-00c04fd430c8")
  (= (time-low tmpid)       [107 167 184 20])
  (= (time-mid tmpid)       [157 173])
  (= (time-high tmpid)      [17 209])
  (= (clk-low tmpid)        [128])
  (= (clk-high tmpid)       [180])
  (= (node tmpid)           [0 192 79 212 48 200])
  )

;; v3 uuid protocol test
(let [tmpid (UUID/fromString "d9c53a66-fde2-3d04-b5ad-dce3848df07e")]
  (= (word-high tmpid)       -2754731383046652668)
  (= (word-low tmpid)        -5355381512134070146)
  (= (null? tmpid)           false)
  (= (to-octet-vector tmpid) [217 197 58 102 253 226 61 4 181 173 220 227 132 141 240 126])
  (= (hash-code tmpid)       963287501)      
  (= (version tmpid)         3)
  (= (to-string tmpid)       "d9c53a66-fde2-3d04-b5ad-dce3848df07e")
  (= (to-hex-string tmpid)   "D9C53A66FDE23D04B5ADDCE3848DF07E")
  (= (to-urn-string tmpid)   "urn:uuid:d9c53a66-fde2-3d04-b5ad-dce3848df07e")
  (= (time-low tmpid)        [217 197 58 102])
  (= (time-mid tmpid)        [253 226])
  (= (time-high tmpid)       [61 4])
  (= (clk-low tmpid)         [181])
  (= (clk-high tmpid)        [173])
  (= (node tmpid)            [220 227 132 141 240 126])
  )

;; v4 uuid protocol test
(let [tmpid (UUID/fromString "3eb1e29a-4747-4a7d-8e40-94e245f57dc0")]
  (= (word-high tmpid)       4517641053478013565)
  (= (word-low tmpid)        -8196387622257066560)
  (= (null? tmpid)           false)
  (= (to-octet-vector tmpid) [62 177 226 154 71 71 74 125 142 64 148 226 69 245 125 192])
  (= (hash-code tmpid)       -1304215099)
  (= (version tmpid)         4)
  (= (to-string tmpid)       "3eb1e29a-4747-4a7d-8e40-94e245f57dc0")
  (= (to-hex-string tmpid)   "3EB1E29A47474A7D8E4094E245F57DC0"   )
  (= (to-urn-string tmpid)   "urn:uuid:3eb1e29a-4747-4a7d-8e40-94e245f57dc0")
  (= (time-low tmpid)        [62 177 226 154])
  (= (time-mid tmpid)        [71 71])
  (= (time-high tmpid)       [74 125])
  (= (clk-low tmpid)         [142])
  (= (clk-high tmpid)        [64])
  (= (node tmpid)            [148 226 69 245 125 192])
  )  


(defn to-uri [uuid]
  (URI/create (str "urn:uuid:" (.toString uuid))))

  
(defn make-v4-uuid []
  (UUID/randomUUID))

(defn make-v0-uuid []
  +null+)

(defn make-null-uuid []
  +null+)

