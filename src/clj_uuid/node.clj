(ns clj-uuid.node
  (:require [clj-uuid.util   :refer [compile-if]])
  (:require [clj-uuid.bitmop :only  [sb8]])
  (:import  [java.net         InetAddress NetworkInterface]
            [java.security    MessageDigest]
            [java.util        Properties]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Internal NodeID Calculation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- all-local-addresses []
  (let [^InetAddress local-host (InetAddress/getLocalHost)
        host-name (.getCanonicalHostName local-host)
        base-addresses #{(str local-host) host-name}
        network-interfaces (reduce (fn [acc ^NetworkInterface ni]
                                     (apply conj acc
                                       (map str (enumeration-seq
                                                  (.getInetAddresses ni)))))
                             base-addresses
                             (enumeration-seq
                               (NetworkInterface/getNetworkInterfaces)))]
    (reduce conj network-interfaces
      (map str (InetAddress/getAllByName host-name)))))


(compile-if (neg? (compare (System/getProperty "java.version") "1.7"))
  ;; JDK6
  (defn- make-node-id []
    (vec (repeatedly 6 #(clj-uuid.bitmop/sb8 (rand-int 256)))))
        
    ;; (clj-uuid.bitmop/ubvec
    ;;   (map clj-uuid.bitmop/ub8
    ;;     (.getHardwareAddress
    ;;       (java.net.NetworkInterface/getByInetAddress 
    ;;         (java.net.InetAddress/getLocalHost))))))

  ;; JDK7
  (defn- make-node-id []
    (let [addresses (all-local-addresses)
          ^MessageDigest digest (MessageDigest/getInstance "MD5")
          ^Properties    props  (System/getProperties)
          to-digest (reduce (fn [acc key]
                              (conj acc (.getProperty props key)))
                      addresses ["java.vendor" "java.vendor.url"
                                 "java.version" "os.arch"
                                 "os.name" "os.version"])]
      (doseq [^String d to-digest]
        (.update digest
          (.getBytes d java.nio.charset.StandardCharsets/UTF_8)))
      (vec (take 6 (seq (.digest digest)))))))
      


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public NodeID API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def node-id   (memoize make-node-id))

(def +node-id+ (node-id))

