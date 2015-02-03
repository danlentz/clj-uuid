(ns clj-uuid.clock)
  
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lock-Free Thread-safe Monotonic Clock
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; we pad to achieve the rfc4122 compat and support atomic incremental
;; uuids-this-second subcounter on the low order bits, guarantee that
;; never collides regardless of clock precision:
;;
;; 113914335216380000    (+ (* (epoch-time) 10000) 100103040000000000)
;; 113914335216380001    first contending timestamp
;; 113914335216380002    second contending timestamp
;; ...                   and so forth
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(set! *warn-on-reflection* true)

(def ^:const +subcounter-resolution+  9999)
(def ^:const +clock-seq+        (+ (rand-int 9999) 1))


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
        (+ (* (.millis new-state) 10000) 100103040000000000)))))


(set! *warn-on-reflection* false)
