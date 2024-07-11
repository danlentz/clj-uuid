(ns clj-uuid.random
  (:require [clj-uuid.bitmop :as bitmop])
  (:import (java.security SecureRandom))
  (:refer-clojure :exclude [bytes long]))

(defonce ^:private secure-random
  (delay (SecureRandom.)))

(defn bytes
  "Generate `n` random bytes."
  [n]
  (let [bs (byte-array n)]
    (.nextBytes ^SecureRandom @secure-random bs)
    bs))

(defn long
  "Generate a long value that is hard to guess. limited to the number of bytes."
  ([]
   (long 8))
  ([n-bytes]
   (reduce (fn [n b] (+ (bit-shift-left n 8) b)) 0 (bytes n-bytes))))

(defn long-12bit
  "Generate a long value between 0 and 512"
  []
  (bit-and (long 3) 0xfff))
