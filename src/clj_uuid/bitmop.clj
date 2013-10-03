(ns clj-uuid.bitmop
  (:use [clojure.core])
  (:use [clojure.pprint])
  (:use [clojure.repl :only [doc find-doc apropos]])
  (:use [clj-uuid.constants])
  (:import (java.net  URI URL))
  (:import (java.util UUID)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc Arithmetic 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn expt [num pow]
  (assert (>= pow 0))
  (loop [acc 1 p pow]
    (if
      (= 0 p) acc
      (recur (* acc num) (- p 1)))))

(defn expt2 [pow]
  (assert (>= pow 0))
  (bit-shift-left 1 pow))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; java.lang.Long Bit Masking
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ub4 [num]
  (bit-and num +ub4-mask+))

(defn ub8 [num]
  (bit-and num +ub8-mask+))

(defn ub16 [num]
  (bit-and num +ub16-mask+))

(defn ub24 [num]
  (bit-and num +ub24-mask+))

(defn ub32 [num]
  (bit-and num +ub32-mask+))

(defn ub48 [num]
  (bit-and num +ub48-mask+))

(defn ub56 [num]
  (bit-and num +ub56-mask+))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; "Octet Vectors" implement a common representation used for unsigned binary
;; aritmetic.  Unsigned integers of a given width 

;; as Vector of Unboxed Shorts 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn svec [coll]
  (apply (partial vector-of :short) coll))

(defn svector [& args]
  (svec args))

(defn- make-vector [length initial-element]
  (svec (loop [len length v []]
         (if (<= len 0)
           v
           (recur (- len 1) (cons initial-element v))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;




(defn octets [num]
  (if (< num 0)
    (svec
      (concat
        (octets (ub32 (bit-shift-right (bit-xor +ub32-mask+ num) 32)))
        (octets (ub32 num))))
    (loop [n num v ()]
      (if (= 0 n)
        (if (empty? v)
          (svector (short 0))
          (svec v))
        (recur (bit-shift-right n 8) (cons (short (ub8 n)) v))))))


(defn word
  ([num]
    (word num 8))
  ([num size]
    (cond
      (number? num) (word (octets num) size)
      (coll? num)   (let [pad (make-vector (- size (count num)) 0)]
                      (svec (concat pad num))))))



(defn words [& nums]
  (svec (apply concat (map word nums))))


(defn- octet-hex [num]
  (str
    (nth hex-chars (ub4 (bit-shift-right num 4)))
    (nth hex-chars (ub4 num))))



(defn hex [thing]
  (cond
    (and (number? thing) (< thing 0))     (hex (octets thing))
    (and (number? thing) (<= thing 0xff)) (octet-hex thing)
    (and (number? thing) (> thing 0xff))  (hex (octets thing))
    (coll? thing)   (apply str (map octet-hex thing))))

