(ns clj-uuid
  "RFC 9562 UUID implementation for Clojure.

  This namespace re-exports all public vars from clj-uuid.core, as Babashka
  does not like single-segment namespaces.    For backward
  compatibility, both `(require '[clj-uuid :as uuid])` and
  `(require '[clj-uuid.core :as uuid])` provide identical functionality.

  Quick start:
    (require '[clj-uuid :as uuid])
    (uuid/v4)       ; random UUID
    (uuid/v7)       ; time-based, sortable, cryptographically secure
    (uuid/v7nc)     ; time-based, sortable, fast (non-cryptographic)

  See clj-uuid.core for complete documentation."
  (:refer-clojure :exclude [== uuid? max < > =])
  (:require [clj-uuid.util :refer [import-vars]]
            [clj-uuid.core :as impl]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Constants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(import-vars impl
  +null+ +max+ +namespace-dns+ +namespace-url+ +namespace-oid+ +namespace-x500+)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Protocols
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def UUIDNameBytes impl/UUIDNameBytes)
(def UUIDable      impl/UUIDable)
(def UUIDRfc9562   impl/UUIDRfc9562)
(def UUIDRfc4122   impl/UUIDRfc4122)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Constructors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(import-vars impl
  null v0 max v1 v3 v4 v5 v6 v7 v7nc v8 squuid)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Protocol Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(import-vars impl
  uuid? null? max? as-uuid uuidable? as-byte-array
  hash-code get-word-high get-word-low get-version get-variant
  get-time-low get-time-mid get-time-high get-clk-low get-clk-high get-clk-seq
  get-node-id get-timestamp get-instant get-unix-time
  to-byte-array to-string to-hex-string to-urn-string to-uri)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Comparison Operators
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(import-vars impl
  uuid= uuid< uuid> = < >)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Predicates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(import-vars impl
  uuid-string? uuid-urn-string? uuid-vec?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clock
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(import-vars impl
  monotonic-time)
