(ns danlentz.clj-uuid.random
  (:refer-clojure :exclude [bytes long])
  (:import (java.security SecureRandom)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Randomness                                  [RFC9562:6.9 "Unguessability"] ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;   "Implementations SHOULD utilize a cryptographically secure
;;   pseudorandom number generator (CSPRNG) to provide values that are
;;   both difficult to predict (unguessable) and have a low likelihood
;;   of collision"
;;
;; UUID variants calling for hard-to-guess random components (currently
;; v7) are generated using java.security.SecureRandom -- A
;; cryptographically strong nondeterministic random number generator
;; that minimally complies with the statistical random number generator
;; tests specified in FIPS 140-2, Security Requirements for
;; Cryptographic Modules, section 4.9.1.

(defonce ^:private secure-random
  (delay (SecureRandom.)))

(defn bytes
  "Generate `n` random bytes."
  [n]
  (let [bs (byte-array n)]
    (.nextBytes ^SecureRandom @secure-random bs) bs))

(defn long
  "Generate a long value that is hard to guess. limited to the number of bytes."
  ([]
   (long 8))
  ([n-bytes]
   (reduce (fn [n b] (+ (bit-shift-left n 8) b)) 0 (bytes n-bytes))))

(defn eight-bits
  "Generate a hard-to-guess long value between 0 and 255"
  []
  (bit-and (long 1) 0xff))
