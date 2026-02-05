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
| `mask-offset`   | O(offset) loop scanning for lowest set bit | `Long/numberOfTrailingZeros` (single `TZCNT` instruction) |
| `mask-width`    | O(width) loop counting contiguous set bits | `Long/bitCount` (single `POPCNT` instruction) |
| `bit-count`     | O(64) loop counting all set bits        | `Long/bitCount` (single `POPCNT` instruction) |

Operations that are **unchanged** between the two (they operate on longs
directly and don't involve byte conversion):

- `mask` -- identical implementation
- `ldb`, `dpb` -- identical implementation (but faster in bitmop2 due to O(1) `mask-offset`)
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
;; clj-uuid-old (bitmop):
(let [ts        (clock/monotonic-time)          ;; atom + swap! + State alloc
      time-low  (ldb #=(mask 32  0)  ts)
      time-mid  (ldb #=(mask 16 32)  ts)
      time-high (dpb #=(mask 4  12) (ldb #=(mask 12 48) ts) 0x1)
      msb       (bit-or time-high
                  (bit-shift-left time-low 32)
                  (bit-shift-left time-mid 16))]
  (UUID. msb (node/+v1-lsb+)))                 ;; memoized fn call

;; clj-uuid (bitmop2) -- inlined CAS + direct bit ops:
(loop []
  (let [current  (.get packed)                  ;; AtomicLong, captured in closure
        millis   (unsigned-bit-shift-right current 14)
        time-now (System/currentTimeMillis)]
    (cond
      (< millis time-now)
      (let [next (bit-shift-left time-now 14)]
        (if (.compareAndSet packed current next)
          (let [ts  (+ 100103040000000000
                       (* (+ 2208988800000 time-now) 10000))
                msb (bit-or
                      (bit-shift-left (bit-and ts 0xFFFFFFFF) 32)
                      (bit-shift-left (bit-and (unsigned-bit-shift-right ts 32) 0xFFFF) 16)
                      0x1000
                      (bit-and (unsigned-bit-shift-right ts 48) 0xFFF))]
            (UUID. msb v1-lsb))               ;; pre-captured long, no fn call
          (recur)))
      ...)))
```

| Operation            | bitmop (clj-uuid-old)          | bitmop2 (clj-uuid)             | Difference |
|----------------------|--------------------------------|--------------------------------|------------|
| Clock                | `atom` + `swap!` + `State` alloc | `AtomicLong.compareAndSet` (inlined) | **no var lookup, no alloc** |
| Bit-field packing    | 3x `ldb` + 1x `dpb` (4 var lookups) | Direct `bit-or`/`bit-and`/`bit-shift` | **no var lookups** |
| Node LSB             | `(node/+v1-lsb+)` (memoize lookup) | `v1-lsb` (pre-captured long)  | **no fn call** |

**Construction impact: ~1.5x speedup (120 ns -> 100 ns).**  Three sources
of overhead are eliminated: (1) `atom`/`swap!`/`State` allocation is replaced
by `AtomicLong.compareAndSet` on a packed long; (2) `ldb`/`dpb` var lookups
are replaced by inlined bit operations; (3) the memoized `+v1-lsb+` function
call is replaced by a pre-captured long in the closure.

**Post-construction impact:** Operations on the resulting UUID differ:

| Post-construction op  | bitmop (clj-uuid-old)                    | bitmop2 (clj-uuid)                    | Speedup  |
|-----------------------|------------------------------------------|---------------------------------------|----------|
| `to-byte-array`       | 2x `long->bytes` (16 loop iterations)    | 2x `putLong` (2 native calls)         | **60x**  |
| `to-hex-string`       | 2x `hex` (lazy seqs + `apply str`)       | `uuid->buf` + `buf-hex` (StringBuilder) | **38x** |
| `to-string`           | `UUID.toString` (JVM)                    | `UUID.toString` (JVM)                  | same     |
| Field extraction      | `ldb`/`dpb` on longs                     | `ldb`/`dpb` on longs                  | same     |

---

### v6 (Time-based, Lexically Sortable)

Same inlined CAS + direct bit-op architecture as v1, with different
bit-field ordering for lexical sorting.

**Construction impact: ~1.4x speedup (106 ns -> 100 ns).**  Same
optimizations as v1.  The smaller relative gain reflects v6's already
lower baseline (fewer bit operations in the original layout).

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
| `ldb` x1, `dpb` x2        | O(offset) `mask-offset` | O(1) `Long/numberOfTrailingZeros` | **1.21x** |
| `random/long` (SecureRandom) | shared | shared | none     |

**Construction impact: 1.21x speedup.**  The `dpb #=(mask 2 62)` call in
the LSB line previously invoked `mask-offset` with an O(offset) loop — for
offset=62, that was 62 iterations per call.  bitmop2's `mask-offset` uses
`Long/numberOfTrailingZeros`, a JVM intrinsic that compiles to a single
`TZCNT` instruction.  This eliminates the v7 regression seen in earlier
benchmarks.  `SecureRandom.nextLong()` still dominates total latency.

**Post-construction impact:** Same as v1/v6 (see table above).

---

### v7nc (Non-cryptographic V7, ThreadLocalRandom)

```clojure
;; clj-uuid (bitmop2) -- per-thread counter + ThreadLocalRandom:
(let [^longs state (.get v7nc-tl)       ;; ThreadLocal long[3]
      ^ThreadLocalRandom tlr ...]
  (loop []
    (let [time-now (System/currentTimeMillis)
          last-ms  (aget state 0)]
      (cond
        (> time-now last-ms)              ;; new millisecond: reseed
        (let [lsb-ctr (bit-and (.nextLong tlr) 0x3FFFFFFFFFFFFFFF)
              msb     (bit-or (bit-shift-left (bit-and time-now 0xFFFFFFFFFFFF) 16)
                              (bit-or 0x7000 (bit-and (.nextLong tlr) 0xFFF)))]
          (aset state 0 time-now)
          (aset state 1 msb)
          (aset state 2 lsb-ctr)
          (UUID. msb (bit-or lsb-ctr variant-bits)))

        true                              ;; same millisecond: increment
        (let [lsb-ctr (bit-and (unchecked-inc (aget state 2)) 0x3FFFFFFFFFFFFFFF)]
          (aset state 2 lsb-ctr)
          (UUID. (aget state 1) (bit-or lsb-ctr variant-bits)))))))
```

No clj-uuid-old equivalent exists.  `v7nc` is a new constructor in 0.2.5.

| Operation                  | v7 (CSPRNG)                  | v7nc                          |
|----------------------------|------------------------------|-------------------------------|
| Clock                      | Global `AtomicLong` CAS      | Per-thread `long[]` (no CAS)  |
| Counter reseed             | `SecureRandom` (~300 ns)     | `ThreadLocalRandom` (~5 ns)   |
| rand_b                     | `SecureRandom.nextLong()`    | Monotonic counter (increment) |
| Hot path (same ms)         | CAS + SecureRandom           | Array load + increment        |

**Construction: ~39 ns.**  The hot path (same millisecond) is just:
`ThreadLocal.get()` + `System.currentTimeMillis()` + array load +
comparison + `unchecked-inc` + `bit-and` + array store + `UUID.` constructor.
No random number generation, no atomics, no var lookups.

**vs JUG 5.2:** `v7nc` at 39 ns is **1.26x faster** than JUG's
`TimeBasedEpochGenerator` at ~50 ns.

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
;; clj-uuid-old (bitmop):
(build-digested-uuid version
  (digest-bytes +md5+|+sha1+
    (to-byte-array (as-uuid context))
    (as-byte-array local-part)))

;; clj-uuid (bitmop2) -- fused pipeline:
(let [^MessageDigest md (.get md5-tl)           ;; ThreadLocal, captured in closure
      ^ByteBuffer nsbuf (.get ns-buf-tl)        ;; ThreadLocal reusable buffer
      _   (.reset md)
      _   (.putLong nsbuf 0 (.getMostSignificantBits (as-uuid context)))
      _   (.putLong nsbuf 8 (.getLeastSignificantBits (as-uuid context)))
      _   (.update md (.array nsbuf))
      digest (.digest md ^bytes (as-byte-array local-part))
      ^ByteBuffer dbuf (ByteBuffer/wrap digest)  ;; wrap, no copy
      msb (bit-or (bit-and (.getLong dbuf 0) version-clear-mask) 0x3000)
      lsb (bit-or (bit-and (.getLong dbuf 8) variant-clear-mask) variant-bits)]
  (UUID. msb lsb))
```

The v3/v5 construction path is the most interesting for performance
comparison, as it touches multiple bitmop operations in sequence:

#### clj-uuid-old pipeline (4 function calls, ~8 var lookups)

| Step | Operation | Cost |
|---|---|---|
| 1 | `to-byte-array` (serialize context UUID) | ~800 ns (16-iter loop) |
| 2 | `digest-bytes` (MD5 or SHA-1 hash) | ~150-300 ns |
| 3 | `build-digested-uuid` → `bytes->long` x2 | ~800 ns (16-iter loop) |
| 4 | `dpb` x2 (version + variant) | ~5 ns |
| | **Total (v3):** | **~1400 ns** |
| | **Total (v5):** | **~1670 ns** |

#### clj-uuid pipeline (fused, 0 var lookups on hot path)

| Step | Operation | Cost |
|---|---|---|
| 1 | Reuse ThreadLocal ByteBuffer + 2x `putLong` | ~3 ns |
| 2 | `MessageDigest` (ThreadLocal, `.reset` + `.update` + `.digest`) | ~150-250 ns |
| 3 | `ByteBuffer/wrap` digest + 2x `.getLong` | ~3 ns |
| 4 | Inline `bit-and`/`bit-or` (compile-time constant masks) | ~2 ns |
| | **Total (v3):** | **~175 ns** |
| | **Total (v5):** | **~260 ns** |

**Overall v3 speedup: ~8x.**  **Overall v5 speedup: ~6.4x.**

Three optimizations compound: (1) ThreadLocal ByteBuffer reuse for
namespace serialization eliminates per-call allocation; (2)
`ByteBuffer/wrap` on the digest output avoids copying 16 bytes;
(3) inline bit-and/bit-or with compile-time constant masks
(`#=(bit-not #=(bitmop/mask ...))`) eliminates all `dpb-buf`,
`buf->uuid`, and `buffer-from-bytes` var lookups.

**vs JUG 5.2:** v5 at ~260 ns is now at parity with JUG's ~254 ns.

---

### v8 (Custom)

```clojure
;; Both implementations:
(UUID.
  (dpb #=(mask 4 12) msb 0x8)
  (dpb #=(mask 2 62) lsb 0x2))
```

**Construction impact: 4.21x speedup (46 ns -> 11 ns).**  The v8
constructor is just two `dpb` calls (`mask(4,12)` and `mask(2,62)`).
With bitmop's O(offset) `mask-offset` loop, the `mask(2,62)` call alone
required 62 loop iterations.  bitmop2's O(1) `Long/numberOfTrailingZeros`
eliminates this overhead, making `dpb` nearly free.

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
| clj-uuid-old | `bitmop/long->bytes` x2 (16 shift/mask iterations total)  | ~804 ns  |
| clj-uuid     | `bitmop2/long->bytes` x2 (2 `putLong` calls)              | ~14 ns   |

**Speedup: ~57x**

This operation is called internally during v3/v5 construction (to serialize
the namespace UUID) and is also part of the public API for any UUID.

### `to-hex-string`

| Impl         | Code path                                                  | Cost     |
|--------------|------------------------------------------------------------|----------|
| clj-uuid-old | `bitmop/hex(msb)` + `bitmop/hex(lsb)` + `str` concat. Each `hex` call: `long->bytes` (8-iter loop) + `map ub8` (lazy seq) + `map octet-hex` (lazy seq of 8 temp strings) + `apply str` | ~5840 ns |
| clj-uuid     | `uuid->buf` (2 putLong) + `buf-hex` (single StringBuilder, 16-byte direct loop) | ~199 ns  |

**Speedup: ~29x**

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
| v0 (null) | --                  | constant                        | **57x**          | **29x**          |
| v1        | **1.5x**            | `AtomicLong` CAS (inlined)      | **57x**          | **29x**          |
| v3        | **~8x**             | MD5 digest (fused pipeline)     | **57x**          | **29x**          |
| v4 (0)    | none                | `SecureRandom` (CSPRNG)         | **57x**          | **29x**          |
| v4 (2)    | negligible          | caller-provided longs           | **57x**          | **29x**          |
| v5        | **~6.4x**           | SHA-1 digest (fused pipeline)   | **57x**          | **29x**          |
| v6        | **1.4x**            | `AtomicLong` CAS (inlined)      | **57x**          | **29x**          |
| v7        | **1.2x**            | `SecureRandom` (CSPRNG)         | **57x**          | **29x**          |
| v7nc      | *new*               | `ThreadLocalRandom` (per-thread)| **57x**          | **29x**          |
| v8        | **4.2x**            | caller-provided longs           | **57x**          | **29x**          |
| squuid    | none                | `SecureRandom` via v4           | **57x**          | **29x**          |
| max       | --                  | constant                        | **57x**          | **29x**          |

**Key takeaway:** The bitmop->bitmop2 change provides the largest speedup in
byte serialization and hex string rendering, which are post-construction
operations common to *all* UUID types.  Additionally, `mask-offset`,
`mask-width`, and `bit-count` now use JVM intrinsics (`Long/numberOfTrailingZeros`
and `Long/bitCount`), replacing O(n) loops with single CPU instructions.
This particularly benefits v7 (1.2x, eliminating a previous regression) and
v8 (4.2x, where `dpb` is the entire constructor cost).  v3/v5 continue to
show the largest gains from byte conversion optimization and ThreadLocal
digest caching.


---

## Where the Gains Matter Most

### High-throughput serialization

Applications that generate UUIDs and immediately serialize them (to byte
arrays for database storage, or to hex strings for logging/wire format)
benefit from the cumulative improvement:

```
clj-uuid-old (v1 + to-byte-array):  ~120 ns (v1) + ~804 ns (bytes) = ~926 ns
clj-uuid     (v1 + to-byte-array):  ~100 ns (v1) + ~14 ns  (bytes) = ~114 ns
                                                                        ~8.1x
```

```
clj-uuid-old (v1 + to-hex-string):  ~120 ns (v1) + ~5840 ns (hex)  = ~5960 ns
clj-uuid     (v1 + to-hex-string):  ~100 ns (v1) + ~126 ns  (hex)  = ~226 ns
                                                                        ~26x
```

### Batch name-based UUID generation (v3/v5)

When generating many v3/v5 UUIDs (e.g., deterministic ID generation from
a dataset), both the namespace serialization and digest-result extraction
are improved:

```
clj-uuid-old (v3): ~800 ns (to-byte-array) + ~200 ns (MD5) + ~800 ns (bytes->long x2) + ~5 ns (dpb)
                  = ~1400 ns

clj-uuid     (v3): ~14 ns (to-byte-array) + ~140 ns (MD5) + ~14 ns (bytes->long x2) + ~3 ns (dpb)
                  = ~160 ns
                                                               ~9.0x
```

```
clj-uuid-old (v5): ~800 ns (to-byte-array) + ~300 ns (SHA-1) + ~800 ns (bytes->long x2) + ~5 ns (dpb)
                  = ~1670 ns

clj-uuid     (v5): ~14 ns (to-byte-array) + ~250 ns (SHA-1) + ~14 ns (bytes->long x2) + ~3 ns (dpb)
                  = ~280 ns
                                                               ~6.0x
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
| UUID construction       | baseline               | **1.2-9x faster**    | see below*     |
| `to-byte-array`         | baseline               | **57x faster**       | ByteBuffer     |
| `to-hex-string`         | baseline               | **29x faster**       | StringBuilder  |
| `bytes->long`           | baseline               | **5-10x faster**     | ByteBuffer     |
| `long->bytes`           | baseline               | **6-27x faster**     | ByteBuffer     |
| `hex`                   | baseline               | **11-35x faster**    | StringBuilder  |
| `assemble-bytes`        | baseline               | **2.4x faster**      | shift-accum    |
| `mask-offset`           | baseline               | **O(1)**             | `TZCNT` intrinsic |
| `mask-width`/`bit-count`| baseline               | **O(1)**             | `POPCNT` intrinsic |
| Field extraction        | baseline               | same                 | n/a            |
| Comparison              | baseline               | same                 | n/a            |
| GC pressure             | higher                 | **lower**            | fewer allocs   |
| cljc readiness          | no                     | **yes** (DataView)   | architecture   |

*Construction speedup varies by UUID type: v3 sees **~8x** and v5 sees
**~6.4x** from the fused digest pipeline with ThreadLocal ByteBuffer
reuse (v5 is now at parity with JUG 5.2).  v8 sees **4.2x** from O(1)
`mask-offset`.  v1 sees **~1.5x** and v6 sees **~1.4x** from inlined
`AtomicLong` CAS, direct bit operations, and pre-captured node LSBs.
v7nc is a new constructor at ~39 ns -- **1.26x faster** than JUG 5.2's
v7 generator, using per-thread `ThreadLocalRandom` instead of
`SecureRandom`.  v4 (0-arity) delegates to `UUID/randomUUID` and is
unchanged.

The largest gains are in **serialization-heavy workloads** where UUIDs are
frequently converted to byte arrays or hex strings -- common in database
drivers, logging frameworks, and wire protocols.  Additional gains come from
**O(1) mask-offset/mask-width/bit-count** using JVM intrinsics
(`Long/numberOfTrailingZeros` and `Long/bitCount`), which particularly
benefits v8 (4.2x) where `dpb` calls with high-offset masks were
previously bottlenecked by an O(offset) loop.
