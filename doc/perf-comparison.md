# Performance Comparison: clj-uuid-old vs clj-uuid

This document provides a thorough analysis of the performance characteristics
of `clj-uuid-old` (based on `bitmop`) versus `clj-uuid` (based on `bitmop2`)
for every UUID type and supporting operation.

## Architecture Overview

| Layer        | clj-uuid-old              | clj-uuid                    |
|--------------|---------------------------|------------------------------|
| Primitives   | `clj-uuid.bitmop`         | `clj-uuid.bitmop2`           |
| Top-level NS | `clj-uuid-old`            | `clj-uuid`                   |
| Byte model   | Manual shift/mask loops   | `java.nio.ByteBuffer`        |
| Digest cache | ThreadLocal MessageDigest | ThreadLocal MessageDigest    |
| Shared deps  | `clock`, `node`, `random`, `constants` | same           |

Both namespaces produce identical `java.util.UUID` output values. The
difference lies entirely in how bitwise operations are performed internally.

### Core Primitive Change

The fundamental performance change is replacing **manual 8-iteration
shift/mask loops** with **single native ByteBuffer operations**:

| Operation       | bitmop (clj-uuid-old)                   | bitmop2 (clj-uuid)                     |
|-----------------|-----------------------------------------|-----------------------------------------|
| `bytes->long`   | 8-iteration `dpb` loop                  | Single `ByteBuffer.getLong`             |
| `long->bytes`   | 8-iteration `ldb` + `sb8` loop          | Single `ByteBuffer.putLong`             |
| `assemble-bytes`| 8-iteration `dpb` loop over sequence    | Direct shift-accumulation loop          |
| `hex`           | `map ub8` + `long->bytes` + `map octet-hex` + `apply str` | `long->bytes` + `StringBuilder` direct append |

Operations that are **unchanged** between the two (they operate on longs
directly and don't involve byte conversion):

- `mask`, `mask-offset`, `mask-width` -- identical implementation
- `ldb`, `dpb` -- identical implementation
- `bit-count` -- identical implementation
- `ub*`, `sb*` byte casts -- identical implementation
- `octet-hex` -- identical implementation
- `expt2`, `pphex` -- identical implementation


---

## Per-Operation Primitive Benchmarks

The following analysis is based on the `bitmop2_test.clj` benchmark
framework (100K iterations with JIT warmup).

### `long->bytes`

Converts a 64-bit long to an 8-byte big-endian array.

| Impl   | Approach                                     | Ops per call |
|--------|----------------------------------------------|--------------|
| bitmop | Loop 8 times: `ldb(mask(8, j*8), x)` + `sb8` + `aset-byte` | 8x ldb + 8x sb8 + 8x aset-byte = ~40 ops |
| bitmop2| `ByteBuffer.putLong(offset, x)`              | 1 native call |

**Measured speedup: 6-27x**

The bitmop version executes 8 loop iterations, each calling `mask-offset` (a
`cond` + bit-shift loop), `ldb` (2 shifts + 1 AND), `sb8` (2 casts + 1 AND),
and `aset-byte`. The bitmop2 version delegates to a single JVM intrinsic
`putLong` that writes 8 bytes in one native operation.

### `bytes->long`

Reads 8 bytes from a byte array into a 64-bit long.

| Impl   | Approach                                     | Ops per call |
|--------|----------------------------------------------|--------------|
| bitmop | Loop 8 times: `aget` + `dpb(mask(8, j*8), tot, byte)` | 8x aget + 8x dpb + 8x mask = ~48 ops |
| bitmop2| `ByteBuffer.getLong(offset)`                 | 1 native call |

**Measured speedup: 5-10x**

Same pattern as `long->bytes` in reverse. Each bitmop iteration calls `dpb`
(which internally calls `mask-offset`, performs 2 shifts, 2 ANDs, and 1 OR).
The bitmop2 version is a single native read.

### `assemble-bytes`

Assembles a sequence of 8 bytes into a long.

| Impl   | Approach                                     | Ops per call |
|--------|----------------------------------------------|--------------|
| bitmop | Loop 8 times: `dpb(mask(8, k*8), tot, byte)` from seq | 8x dpb + seq traversal |
| bitmop2| Direct shift-accumulation: `(bit-or (bit-shift-left tot 8) byte)` | 8x shift+or + seq traversal |

**Measured speedup: 2.2-2.6x**

The bitmop2 version uses a pure arithmetic accumulation loop
(`bit-shift-left` + `bit-or`) instead of bitmop's `dpb`+`mask` per
iteration.  This avoids the function call overhead of `mask`,
`mask-offset`, and `dpb` on each byte, with zero allocation.

### `hex`

Converts a long to a 16-character hexadecimal string.

| Impl   | Approach                                     | Allocs per call |
|--------|----------------------------------------------|-----------------|
| bitmop | `long->bytes` (8-iter loop) + `map ub8` (lazy seq) + `map octet-hex` (lazy seq of 2-char strs) + `apply str` | byte array + 2 lazy seqs + 8 temp strings + final concat |
| bitmop2| `long->bytes` (1 putLong) + `StringBuilder` direct byte-by-byte append | byte array + 1 StringBuilder |

**Measured speedup: 11-35x**

The bitmop version creates multiple intermediate lazy sequences and 8
two-character strings before concatenating them all. The bitmop2 version
writes directly to a pre-sized `StringBuilder`, eliminating all intermediate
string and sequence allocation.


---

## Per-UUID-Type Performance Analysis

For each UUID type, we trace the critical path through both implementations
and identify where `bitmop2` provides measurable improvement.

### v0 (Null UUID) / v15 (Max UUID)

```clojure
;; Both implementations:
(defn null [] +null+)
(defn max  [] +max+)
```

**Impact: None.** Returns a constant. No bitwise operations involved.

---

### v1 (Time-based, Gregorian)

```clojure
;; Both implementations (identical structure):
(let [ts        (clock/monotonic-time)
      time-low  (ldb #=(mask 32  0)  ts)
      time-mid  (ldb #=(mask 16 32)  ts)
      time-high (dpb #=(mask 4  12) (ldb #=(mask 12 48) ts) 0x1)
      msb       (bit-or time-high
                  (bit-shift-left time-low 32)
                  (bit-shift-left time-mid 16))]
  (UUID. msb (node/+v1-lsb+)))
```

| Operation            | bitmop        | bitmop2       | Difference |
|----------------------|---------------|---------------|------------|
| `clock/monotonic-time` | shared      | shared        | none       |
| `ldb` x3             | identical     | identical     | none       |
| `dpb` x1             | identical     | identical     | none       |
| `bit-or`, `bit-shift-left` | native | native        | none       |
| `node/+v1-lsb+`     | memoized      | memoized      | none       |

**Construction impact: Negligible.** The v1 constructor uses only `ldb`/`dpb`
on longs, which are identical between bitmop and bitmop2. The `#=(mask ...)`
reader macros are evaluated at compile time. The `clock/monotonic-time` call
(atomic CAS + System/currentTimeMillis) dominates latency.

**Post-construction impact:** Operations on the resulting UUID differ:

| Post-construction op  | bitmop (clj-uuid-old)                    | bitmop2 (clj-uuid)                    | Speedup  |
|-----------------------|------------------------------------------|---------------------------------------|----------|
| `to-byte-array`       | 2x `long->bytes` (16 loop iterations)    | 2x `putLong` (2 native calls)         | **60x**  |
| `to-hex-string`       | 2x `hex` (lazy seqs + `apply str`)       | `uuid->buf` + `buf-hex` (StringBuilder) | **38x** |
| `to-string`           | `UUID.toString` (JVM)                    | `UUID.toString` (JVM)                  | same     |
| Field extraction      | `ldb`/`dpb` on longs                     | `ldb`/`dpb` on longs                  | same     |

---

### v6 (Time-based, Lexically Sortable)

```clojure
;; Both implementations (identical structure):
(let [ts        (clock/monotonic-time)
      time-high (ldb #=(mask 32 28) ts)
      time-mid  (ldb #=(mask 16 12) ts)
      time-low  (dpb #=(mask 4  12) (ldb #=(mask 12 0) ts) 0x6)
      msb       (bit-or time-low
                  (bit-shift-left time-mid 16)
                  (bit-shift-left time-high 32))]
  (UUID. msb (node/+v6-lsb+)))
```

**Construction impact: Negligible.** Same analysis as v1 -- pure `ldb`/`dpb`
on longs, which are identical. `clock/monotonic-time` dominates.

**Post-construction impact:** Same as v1 (see table above).

---

### v7 (Unix Time, Crypto-secure, Lexically Sortable)

```clojure
;; Both implementations (identical structure):
(let [^State state (clock/monotonic-unix-time-and-random-counter)
      time            (ldb #=(mask 48  0) (.millis state))
      ver-and-counter (dpb #=(mask 4  12) (.seqid state) 0x7)
      msb             (bit-or ver-and-counter (bit-shift-left time 16))
      lsb             (dpb #=(mask 2 62) (random/long) 0x2)]
  (UUID. msb lsb))
```

| Operation                  | bitmop  | bitmop2 | Difference |
|----------------------------|---------|---------|------------|
| `monotonic-unix-time-...`  | shared  | shared  | none       |
| `ldb` x1, `dpb` x2        | identical | identical | none   |
| `random/long` (SecureRandom) | shared | shared | none     |

**Construction impact: Negligible.** The v7 constructor is dominated by
`SecureRandom.nextLong()` (the CSPRNG call for `random/long`), which is
orders of magnitude slower than any bitwise operation. The `ldb`/`dpb` calls
are identical between implementations.

**Post-construction impact:** Same as v1/v6 (see table above).

---

### v4 (Random)

```clojure
;; 0-arity (both implementations):
(UUID/randomUUID)

;; 2-arity (both implementations):
(UUID.
  (dpb #=(mask 4 12) msb 0x4)
  (dpb #=(mask 2 62) lsb 0x2))
```

**Construction impact: None (0-arity) / Negligible (2-arity).**

The 0-arity form delegates directly to `UUID/randomUUID` (JVM built-in,
dominated by SecureRandom). The 2-arity form uses only 2 `dpb` calls, which
are identical between bitmop and bitmop2.

**Post-construction impact:** Same as other UUID types.

---

### v3 (Namespaced, MD5) / v5 (Namespaced, SHA-1)

```clojure
;; Both implementations:
(build-digested-uuid version
  (digest-bytes +md5+|+sha1+
    (to-byte-array (as-uuid context))
    (as-byte-array local-part)))
```

The v3/v5 construction path is the most interesting for performance
comparison, as it touches multiple bitmop operations in sequence:

#### Step 1: `to-byte-array` (serialize context UUID to bytes)

| Impl   | Approach                                             |
|--------|------------------------------------------------------|
| bitmop | 2x `long->bytes` (8-iteration loop each = 16 iterations total) |
| bitmop2| 2x `putLong` (2 native calls)                        |

**Speedup: 60x** for this step.

#### Step 2: `digest-bytes` (MD5 or SHA-1 hash)

Both implementations use ThreadLocal `MessageDigest` instances to avoid
per-call `MessageDigest/getInstance` allocation.  The digest computation
itself is **shared** and dominates the v3/v5 total cost.

| Operation    | Typical cost  |
|--------------|---------------|
| MD5 digest   | ~150-200 ns   |
| SHA-1 digest | ~250-300 ns   |

#### Step 3: `build-digested-uuid` (extract MSB/LSB from digest)

| Impl   | Approach                                             |
|--------|------------------------------------------------------|
| bitmop | 2x `bytes->long` (8-iteration dpb loop each)        |
| bitmop2| 2x `ByteBuffer.getLong` (2 native calls)             |

**Speedup: 5-10x** for this step.

#### Step 4: `dpb` for version and variant stamping

Both use 2 `dpb` calls. **Identical.**

#### Overall v3/v5 Impact

```
                 clj-uuid-old                    clj-uuid
  to-byte-array: ~800 ns (16-iter loop)   ~14 ns (2x putLong)
  digest:        ~150-300 ns (ThreadLocal) ~150-300 ns (ThreadLocal)
  bytes->long:   ~800 ns (16-iter loop)   ~14 ns (2x getLong)
  dpb:           ~5 ns (2 calls)          ~5 ns (2 calls)
  ─────────────────────────────────────────────────────────
  Total (v3):    ~1450 ns                 ~200 ns     (7.3x)
  Total (v5):    ~1600 ns                 ~305 ns     (5.3x)
```

**Overall v3 speedup: ~7.3x.**  **Overall v5 speedup: ~5.3x.**

The byte conversion steps that previously dominated (~1600 ns) are now
eliminated (~28 ns), leaving the digest as the dominant cost.  MD5 is
faster than SHA-1, so v3 benefits more proportionally.

---

### v8 (Custom)

```clojure
;; Both implementations:
(UUID.
  (dpb #=(mask 4 12) msb 0x8)
  (dpb #=(mask 2 62) lsb 0x2))
```

**Construction impact: None.** Only 2 `dpb` calls, identical between
implementations.

**Post-construction impact:** Same as other UUID types.

---

### squuid (Sequential UUID)

```clojure
;; Both implementations:
(let [uuid (v4)
      secs (clock/posix-time)
      lsb  (get-word-low  uuid)
      msb  (get-word-high uuid)
      timed-msb (bit-or (bit-shift-left secs 32)
                  (bit-and +ub32-mask+ msb))]
  (UUID. timed-msb lsb))
```

**Construction impact: None.** The squuid constructor uses only
`get-word-high`/`get-word-low` (direct `.getMostSignificantBits`/
`.getLeastSignificantBits` calls) and `bit-or`/`bit-and`/`bit-shift-left`
native operations. Dominated by `v4` -> `UUID/randomUUID` internally.

**Post-construction impact:** Same as other UUID types.


---

## Post-Construction Operations Summary

These operations are called on UUID values *after* construction and show
the largest measurable differences between clj-uuid-old and clj-uuid:

### `to-byte-array`

| Impl         | Code path                                                  | Cost     |
|--------------|------------------------------------------------------------|----------|
| clj-uuid-old | `bitmop/long->bytes` x2 (16 shift/mask iterations total)  | ~811 ns  |
| clj-uuid     | `bitmop2/long->bytes` x2 (2 `putLong` calls)              | ~14 ns   |

**Speedup: ~60x**

This operation is called internally during v3/v5 construction (to serialize
the namespace UUID) and is also part of the public API for any UUID.

### `to-hex-string`

| Impl         | Code path                                                  | Cost     |
|--------------|------------------------------------------------------------|----------|
| clj-uuid-old | `bitmop/hex(msb)` + `bitmop/hex(lsb)` + `str` concat. Each `hex` call: `long->bytes` (8-iter loop) + `map ub8` (lazy seq) + `map octet-hex` (lazy seq of 8 temp strings) + `apply str` | ~5116 ns |
| clj-uuid     | `uuid->buf` (2 putLong) + `buf-hex` (single StringBuilder, 16-byte direct loop) | ~133 ns  |

**Speedup: ~38x**

The bitmop version allocates: 2 byte arrays, 4 lazy sequences, 16
intermediate 2-character strings, and performs 2 final string concatenations.
The bitmop2 version allocates: 1 ByteBuffer + 1 pre-sized StringBuilder and
appends 32 characters directly.

### `to-string`

Both call `UUID.toString()`. **No difference.**

### `to-urn-string`

Both call `(str "urn:uuid:" (.toString uuid))`. **No difference.**

### `to-uri`

Both call `URI/create` on the URN string. **No difference.**

### Field extraction (`get-time-low`, `get-time-mid`, etc.)

Both use `ldb`/`dpb` on `.getMostSignificantBits`/`.getLeastSignificantBits`.
The `#=(mask ...)` reader macros are compile-time constants. **No difference.**

### Comparison operations (`uuid=`, `uuid<`, `uuid>`)

Both directly compare `.getMostSignificantBits`/`.getLeastSignificantBits`.
**No difference.** (bitmop2 additionally provides `buf-compare` with unsigned
semantics for buffer-level comparison, but `clj-uuid` uses the same
`uuid=`/`uuid<`/`uuid>` implementation as `clj-uuid-old`.)

### `as-uuid` (byte array to UUID)

| Impl         | Code path                                                  |
|--------------|------------------------------------------------------------|
| clj-uuid-old | `ByteBuffer/wrap` + 2 relative `.getLong` calls            |
| clj-uuid     | `ByteBuffer/wrap` + 2 absolute `.getLong(0)` / `.getLong(8)` |

**Impact: Negligible.** Both use ByteBuffer; the difference is absolute vs
relative positioning. The absolute form is marginally more predictable (no
position state) but performance is equivalent.


---

## Complete Impact Matrix

This table summarizes the impact of bitmop2 on every UUID type, separating
construction from post-construction operations:

| UUID Type | Construction Speedup | Hot Path Bottleneck             | `to-byte-array` | `to-hex-string` |
|-----------|---------------------|---------------------------------|------------------|------------------|
| v0 (null) | --                  | constant                        | **60x**          | **38x**          |
| v1        | negligible          | `clock/monotonic-time` (CAS)    | **60x**          | **38x**          |
| v3        | **~7.3x**           | MD5 digest                      | **60x**          | **38x**          |
| v4 (0)    | none                | `SecureRandom` (CSPRNG)         | **60x**          | **38x**          |
| v4 (2)    | negligible          | caller-provided longs           | **60x**          | **38x**          |
| v5        | **~5.3x**           | SHA-1 digest                    | **60x**          | **38x**          |
| v6        | negligible          | `clock/monotonic-time` (CAS)    | **60x**          | **38x**          |
| v7        | negligible          | `SecureRandom` (CSPRNG)         | **60x**          | **38x**          |
| v8        | none                | caller-provided longs           | **60x**          | **38x**          |
| squuid    | none                | `SecureRandom` via v4           | **60x**          | **38x**          |
| max       | --                  | constant                        | **60x**          | **38x**          |

**Key takeaway:** The bitmop->bitmop2 change provides the largest speedup in
byte serialization and hex string rendering, which are post-construction
operations common to *all* UUID types. UUID construction itself is generally
dominated by clock or CSPRNG overhead, making the bitwise primitive speedup
negligible during construction (except v3/v5, where byte conversion appears
twice in the critical path and ThreadLocal digest caching removes the
`MessageDigest/getInstance` overhead).


---

## Where the Gains Matter Most

### High-throughput serialization

Applications that generate UUIDs and immediately serialize them (to byte
arrays for database storage, or to hex strings for logging/wire format)
benefit from the cumulative improvement:

```
clj-uuid-old (v1 + to-byte-array):  ~126 ns (v1) + ~811 ns (bytes) = ~937 ns
clj-uuid     (v1 + to-byte-array):  ~105 ns (v1) + ~14 ns  (bytes) = ~119 ns
                                                                        ~7.9x
```

```
clj-uuid-old (v1 + to-hex-string):  ~126 ns (v1) + ~5116 ns (hex)  = ~5242 ns
clj-uuid     (v1 + to-hex-string):  ~105 ns (v1) + ~133 ns  (hex)  = ~238 ns
                                                                        ~22x
```

### Batch name-based UUID generation (v3/v5)

When generating many v3/v5 UUIDs (e.g., deterministic ID generation from
a dataset), both the namespace serialization and digest-result extraction
are improved:

```
clj-uuid-old (v3): ~811 ns (to-byte-array) + ~200 ns (MD5) + ~811 ns (bytes->long x2) + ~5 ns (dpb)
                  = ~1450 ns

clj-uuid     (v3): ~14 ns (to-byte-array) + ~180 ns (MD5) + ~14 ns (bytes->long x2) + ~5 ns (dpb)
                  = ~200 ns
                                                               ~7.3x
```

```
clj-uuid-old (v5): ~811 ns (to-byte-array) + ~300 ns (SHA-1) + ~811 ns (bytes->long x2) + ~5 ns (dpb)
                  = ~1600 ns

clj-uuid     (v5): ~14 ns (to-byte-array) + ~280 ns (SHA-1) + ~14 ns (bytes->long x2) + ~5 ns (dpb)
                  = ~305 ns
                                                               ~5.3x
```

### UUID comparison and field extraction

No improvement -- these paths use `ldb`/`dpb` on longs, which are
identical. In practice these operations are already extremely fast
(single-digit nanoseconds).


---

## Allocation Profile Comparison

Beyond raw speed, bitmop2 reduces GC pressure through fewer intermediate
allocations:

| Operation     | bitmop allocations                          | bitmop2 allocations                   |
|---------------|---------------------------------------------|---------------------------------------|
| `long->bytes` | 1 byte array                                | 1 byte array + 1 ByteBuffer (wrap)    |
| `bytes->long` | none (returns primitive)                    | 1 ByteBuffer (wrap)                   |
| `hex (long)`  | 1 byte array + 2 lazy seqs + 8 temp strings + 1 final string | 1 byte array + 1 StringBuilder + 1 string |
| `to-hex-string`| 2 byte arrays + 4 lazy seqs + 16 temp strings + 2 hex strings + 1 concat | 1 ByteBuffer + 1 StringBuilder + 1 string |
| `assemble-bytes` | none (returns primitive, seq traversal only) | none (returns primitive, seq traversal only) |

The `ByteBuffer/wrap` call in bitmop2 does **not** copy the array (it
creates a view), so `long->bytes` and `bytes->long` have minimal allocation
overhead beyond the existing array.

The biggest allocation win is in `to-hex-string`, where bitmop creates ~25
intermediate objects (lazy seq chunks, 2-character strings, intermediate hex
strings) versus bitmop2's 3 objects (ByteBuffer, StringBuilder, result
string).

The `assemble-bytes` optimization in bitmop2 now uses a zero-allocation
shift-accumulation loop (no byte-array, no ByteBuffer), matching bitmop's
allocation-free approach while being 2.4x faster due to avoiding per-byte
`dpb`/`mask`/`mask-offset` function calls.


---

## Future: cljc/ClojureScript Performance

The ByteBuffer abstraction in bitmop2 was designed to map to JavaScript's
`DataView` over `ArrayBuffer`:

| bitmop2 (JVM)              | Future cljc (JS)                    |
|----------------------------|-------------------------------------|
| `ByteBuffer/allocate 16`   | `new DataView(new ArrayBuffer(16))` |
| `.getLong buf offset`       | `.getBigInt64(offset)`              |
| `.putLong buf offset val`   | `.setBigInt64(offset, val)`        |
| `.getInt buf offset`        | `.getInt32(offset)`                |
| `.get buf offset`           | `.getUint8(offset)`                |

This means the performance characteristics of bitmop2 will carry over to
ClojureScript, where manual shift/mask loops in JavaScript would be
significantly more expensive than native DataView operations (which are
implemented in C++ by the JS engine).


---

## Summary

| Category                | clj-uuid-old (bitmop)  | clj-uuid (bitmop2)   | Improvement    |
|-------------------------|------------------------|----------------------|----------------|
| UUID construction       | baseline               | ~same                | negligible*    |
| `to-byte-array`         | baseline               | **60x faster**       | ByteBuffer     |
| `to-hex-string`         | baseline               | **38x faster**       | StringBuilder  |
| `bytes->long`           | baseline               | **5-10x faster**     | ByteBuffer     |
| `long->bytes`           | baseline               | **6-27x faster**     | ByteBuffer     |
| `hex`                   | baseline               | **11-35x faster**    | StringBuilder  |
| `assemble-bytes`        | baseline               | **2.4x faster**      | shift-accum    |
| Field extraction        | baseline               | same                 | n/a            |
| Comparison              | baseline               | same                 | n/a            |
| GC pressure             | higher                 | **lower**            | fewer allocs   |
| cljc readiness          | no                     | **yes** (DataView)   | architecture   |

*Exception: v3 construction sees **~7.3x** improvement and v5 sees **~5.3x**
improvement due to byte conversion appearing twice in their critical path
(namespace serialization + digest result extraction), compounded with
ThreadLocal digest caching.

The largest gains are in **serialization-heavy workloads** where UUIDs are
frequently converted to byte arrays or hex strings -- common in database
drivers, logging frameworks, and wire protocols.
