(ns clj-uuid.node
  (:require [clj-uuid.util   :refer [java6? compile-if]])
  (:require [clj-uuid.bitmop :refer [sb8]])
  (:import  [java.net         InetAddress NetworkInterface]
            [java.security    MessageDigest]
            [java.util        Properties]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Internal NodeID Calculation
;;
;; This turns out to be surprisingly problematic.  I've tried various
;; approaches.  The most straightforward:
;;
;;     (.getHardwareAddress
;;       (java.net.NetworkInterface/getByInetAddress 
;;         (java.net.InetAddress/getLocalHost))))))
;;
;; Unfortunately got reports of NPE on some platforms (openjdk?).  Also, it
;; discloses the hardware address of the host system -- this is how the 
;; creator of the melissa virus was actually tracked down and caught.
;;
;; choosing node-id randomly does not provide consistent generation of UUID's
;; across runtimes.
;;
;; Credit for this eventual solution goes to Datastax and @jjcomer for
;; submitting the patch.
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
        (compile-if (java6?)
          (.update digest (.getBytes d))
          (.update digest
            (.getBytes d java.nio.charset.StandardCharsets/UTF_8))))
      (take 6
        (seq (.digest digest)))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public NodeID API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def node-id   (memoize make-node-id))

(def +node-id+ (node-id))

