(ns clj-uuid.clock)


(def ^:const +tick-resolution+  9999)

(def state              (atom {:this-tick 0 :last-time 0}))
(def clock-seq          (atom (+ (rand-int 9999) 1)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; we pad to achieve the rfc4122 compat and support atomic incremental
;; uuids-this-second subcounter on the low order bits, guarantee that never
;; collides regardless of clock precision:
;;
;; 113914335216380000  = (+ (* (get-epoch-time)   10000)    100103040000000000)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn monotonic-time []
  (letfn [(timestamp []
            (+ (* (System/currentTimeMillis) 10000)  100103040000000000))]
      (apply + ((juxt :last-time :this-tick)
                (swap! state
                  #(loop [time-now (timestamp)]
                     (if-not (= (:last-time %1) time-now)
                       {:this-tick 0
                        :last-time time-now}
                       (let [tt (:this-tick %1)] 
                         (if (< tt +tick-resolution+)
                           {:this-tick (inc tt)
                            :last-time time-now}
                           (recur (timestamp)))))))))))


