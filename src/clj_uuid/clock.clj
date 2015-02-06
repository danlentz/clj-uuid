(ns clj-uuid.clock)
  
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lock-Free Thread-safe Monotonic Clock
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Universal time is represented as the number of seconds that have
;; elapsed since 00:00 January 1, 1900 GMT.

;; POSIX time is represented as the number of s
;; between the current time and midnight, January 1, 1970 UTC

;; Java time is represented as the difference, measured in milliseconds,
;; between the current time and midnight, January 1, 1970 UTC

;; UUIDs use Gregorian epoch, 12am Friday October 15, 1582 UTC

;; Difference between Gregorian epoch and Java Epoch = 141427 days =
;; 12219292800 seconds

;; we pad to achieve the rfc4122 compat and support atomic incremental
;; uuids-this-second subcounter on the low order bits, guarantee that
;; never collides regardless of clock precision:
;;
;; 113914335216380000  (+ (* (universal-time) 10000) 100103040000000000)
;; 113914335216380001  first contending timestamp
;; 113914335216380002  second contending timestamp
;; ...                 and so forth
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(set! *warn-on-reflection* true)

(def ^:const +subcounter-resolution+    9999)
(def ^:const +clock-seq+ (inc (rand-int 9999)))

(deftype State [^short seqid ^long millis])


(let [-state- (atom (->State 0 0))]
  (defn monotonic-time ^long []
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
      (+ (.seqid new-state)       
        (+ 100103040000000000
          (* (+ 2208988800000 (.millis new-state)) 10000))))))


(defn universal-time []
  (+ (quot (System/currentTimeMillis) 1000) 2208988800))


  
(set! *warn-on-reflection* false)
