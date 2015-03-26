(ns clj-uuid.clock)
  
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lock-Free, Thread-safe Monotonic Clock
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Timestamp Epochs                       [RFC4122:4.1.4 "TIMESTAMP"] ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;   Universal time is represented as the number of seconds that have
;;   elapsed since 00:00 January 1, 1900 GMT.
;;
;;   POSIX time is represented as the number of seconds that have 
;;   elaspsed since 00:00 January 1, 1970 UTC
;;
;;   Java time is represented as the difference, measured in milliseconds,
;;   between the current time and midnight, January 1, 1970 UTC
;;
;;   UUIDs use Gregorian epoch, 12am Friday October 15, 1582 UTC
;;
;;   Difference between Gregorian epoch and Java Epoch = 141427 days =
;;   12219292800 seconds

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clock Resolution       [RFC4122:4.2.1.2 "SYSTEM CLOCK RESOLUTION"] ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;   "The timestamp is generated from the system time, whose resolution may
;;   be less than the resolution of the UUID timestamp... If a system
;;   overruns the generator by requesting too many UUIDs within a single
;;   system time interval, the UUID service MUST... stall the UUID generator
;;   until the system clock catches up.
;;
;;   A high resolution timestamp can be simulated by keeping a count of
;;   the number of UUIDs that have been generated with the same value of
;;   the system time, and using it to construct the low order bits of the
;;   timestamp.  The count will range between zero and the number of
;;   100-nanosecond intervals per system time interval."
;;
;; We implement this general theory of operation, but make use of Clojure's
;; native lock-free concurrency primatives in order to ensure thread safety.
;; The system time is adjusted to achieve the rfc4122 gregorian timestamp and
;; and combined with the value read from a lock-free, atomic
;; incremental timestamps-this-tick subcounter on the low order bits.
;; This ensures no two monotonic timestamps ever collide, regardless of
;; system clock precision or degree of concurrency:
;;
;; 113914335216380000  (+ (* (universal-time) 10000) 100103040000000000)
;; 113914335216380001  first contending timestamp
;; 113914335216380002  second contending timestamp
;; ...                 and so forth
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(set! *warn-on-reflection* true)

(def  +subcounter-resolution+    9999)

(deftype State [^short seqid ^long millis])


(let [-state- (atom (->State 0 0))]
  (defn monotonic-time []
     (let [^State new-state
           (swap! -state-
             (fn [^State current-state]
               (loop [time-now (System/currentTimeMillis)]
                 (if-not (= (.millis current-state) time-now)
                   (->State 0 time-now)
                   (let [tt (.seqid current-state)]
                     (if (< tt +subcounter-resolution+)
                       (->State (inc tt) time-now)
                       (recur (System/currentTimeMillis))))))))]
       (+ (.seqid new-state) 100103040000000000
         (* (+ 2208988800000 (.millis new-state)) 10000)))))


(defn posix-time
  ([]
   (posix-time (System/currentTimeMillis)))
  ([^long gregorian]
   (- (quot gregorian 10000) 12219292800000)))

(defn universal-time
  ([]
   (universal-time (monotonic-time)))
  ([^long gregorian]
   (+ (posix-time gregorian) 2208988800)))
  
(set! *warn-on-reflection* false)
