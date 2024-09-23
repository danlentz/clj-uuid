(ns clj-uuid.clock
  (:require [clj-uuid.random :as random]))

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

(deftype State [^long seqid ^long millis])

(def ^:const +subcounter-resolution+     9999)

(let [-state- (atom (->State 0 0))]
  (defn monotonic-time []
     (let [^State new-state
           (swap! -state-
             (fn [^State current-state]
               (loop []
                 (let [time-now (System/currentTimeMillis)]
                   (cond
                     (< (.millis current-state) time-now)
                     (->State 0 time-now)

                     (> (.millis current-state) time-now)
                     (recur)

                     true
                     (let [tt (inc (.seqid current-state))]
                       (if (<= tt +subcounter-resolution+)
                         (->State tt time-now)
                         (recur))))))))]
       (+ (.seqid new-state) 100103040000000000
          (* (+ 2208988800000 (.millis new-state)) 10000)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Monotonicity and Counters       [RFC9562:6.2.2 "Monotonic Random"] ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;   "With this method, the random data is extended to also function as
;;   a counter. This monotonic value can be thought of as a "randomly
;;   seeded counter" that MUST be incremented in the least significant
;;   position for each UUID created on a given timestamp tick. UUIDv7's
;;   rand_b section SHOULD be utilized with this method to handle batch
;;   UUID generation during a single timestamp tick. The increment value
;;   for every UUID generation is a random integer of any desired length
;;   larger than zero. It ensures that the UUIDs retain the required level
;;   of unguessability provided by the underlying entropy. The increment
;;   value MAY be 1 when the number of UUIDs generated in a particular
;;   period of time is important"

(def ^:const +random-counter-resolution+ 0xfff)

(let [-state- (atom (->State 0 0))]
  (defn monotonic-unix-time-and-random-counter []
     (let [^State new-state
           (swap! -state-
             (fn [^State current-state]
               (loop []
                 (let [time-now (System/currentTimeMillis)]
                   (cond
                     (< (.millis current-state) time-now)
                     (->State (random/long-12bit) time-now)

                     (> (.millis current-state) time-now)
                     (recur)

                     true
                     (let [tt (inc (.seqid current-state))]
                       (if (<= tt +random-counter-resolution+)
                         (->State tt time-now)
                         (recur))))))))]
       [(.millis new-state) (.seqid new-state)])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Time Utilities
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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






(comment
  (require '[criterium.core :as criterium :refer [bench]])


;; (let [-state- (atom (->State 0 0))]
;;   (defn monotonic-unix-time-and-counter []
;;     (let [^State new-state
;;           (swap! -state-
;;                  (fn [^State current-state]
;;                    (loop [time-now (java.time.Instant/now)]
;;                      (let [time-now-epoch-millis (.toEpochMilli time-now)
;;                            nanos (.getNano time-now)
;;                            nanos-till-ms (min 999999 (- 1000000 (rem nanos 1000000)))]
;;                        (if-not (= (.millis current-state) time-now-epoch-millis)
;;                          (->State 0 time-now-epoch-millis)
;;                          (let [tt (.seqid current-state)]
;;                            (if (< tt +random-counter-resolution+)
;;                              (->State (inc tt) time-now-epoch-millis)
;;                              (do ;; recur when counter is out of runway - sleep until new millisecond
;;                                (java.lang.Thread/sleep 0 nanos-till-ms)
;;                                (recur (java.time.Instant/now))))))))))]
;;       [(.millis new-state) (.seqid new-state)])))


  (bench (and (repeatedly 100000000 monotonic-unix-time-and-random-counter) nil))

  ;; Evaluation count : 28688470740 in 60 samples of 478141179 calls.
  ;;              Execution time mean : 0.414950 ns
  ;;     Execution time std-deviation : 0.044658 ns
  ;;    Execution time lower quantile : 0.379115 ns ( 2.5%)
  ;;    Execution time upper quantile : 0.501328 ns (97.5%)
  ;;                    Overhead used : 1.640844 ns



  (bench (and (repeatedly 100000000 monotonic-unix-time-and-counter) nil))

  ;; Evaluation count : 17791729260 in 60 samples of 296528821 calls.
  ;;              Execution time mean : 1.794180 ns
  ;;     Execution time std-deviation : 0.074041 ns
  ;;    Execution time lower quantile : 1.730718 ns ( 2.5%)
  ;;    Execution time upper quantile : 1.930633 ns (97.5%)
  ;;                    Overhead used : 1.640844 ns
  )
