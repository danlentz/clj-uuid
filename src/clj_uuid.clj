(ns clj-uuid
  "DEPRECATED: use clj-uuid.core"
  {:deprecated "0.2.1"
   :superseded-by "clj-uuid.core"
   :no-doc true}
  (:refer-clojure :exclude [== uuid? max < > =])
  (:require [clj-uuid.core :as uuid]))

(def UUIDNameBytes uuid/UUIDNameBytes)
(def UUIDable      uuid/UUIDable)
(def UUIDRfc4122   uuid/UUIDRfc4122)
(def UUIDRfc9562   uuid/UUIDRfc9562)

(def monotonic-time uuid/monotonic-time)
(def v0 uuid/v0)
(def v1 uuid/v1)
(def v3 uuid/v3)
(def v4 uuid/v4)
(def v5 uuid/v5)
(def v6 uuid/v6)
(def v7 uuid/v7)
(def v8 uuid/v8)
(def squuid uuid/squuid)

(def hash-code uuid/hash-code)
(def = uuid/=)
(def < uuid/<)
(def > uuid/>)
(def uuid= uuid/uuid=)
(def uuid> uuid/uuid>)
(def uuid< uuid/uuid<)

(def get-instant uuid/get-instant)
(def get-unix-time uuid/get-unix-time)
(def get-timestamp uuid/get-timestamp)

(def to-hex-string uuid/to-hex-string)
(def to-byte-array uuid/to-byte-array)
(def to-urn-string uuid/to-urn-string)
(def to-string uuid/to-string)
(def to-uri uuid/to-uri)
(def as-uuid uuid/as-uuid)
(def as-byte-array uuid/as-byte-array)

(def get-word-low uuid/get-word-low)
(def get-clk-seq uuid/get-clk-seq)
(def get-clk-low uuid/get-clk-low)
(def get-node-id uuid/get-node-id)
(def get-word-high uuid/get-word-high)
(def get-clk-high uuid/get-clk-high)
(def get-time-low uuid/get-time-low)
(def get-version uuid/get-version)
(def get-time-high uuid/get-time-high)
(def get-time-mid uuid/get-time-mid)
(def get-variant uuid/get-variant)

(def uuid? uuid/uuid?)
(def uuid-vec? uuid/uuid-vec?)
(def uuid-string? uuid/uuid-string?)
(def uuid-urn-string? uuid/uuid-urn-string?)
(def uuidable? uuid/uuidable?)

(def +max+ uuid/+max+)
(def max uuid/max)
(def max? uuid/max?)

(def +null+ uuid/+null+)
(def null uuid/null)
(def null? uuid/null?)

(def +namespace-oid+ uuid/+namespace-oid+)
(def +namespace-url+ uuid/+namespace-url+)
(def +namespace-x500+ uuid/+namespace-x500+)
(def +namespace-dns+ uuid/+namespace-dns+)
