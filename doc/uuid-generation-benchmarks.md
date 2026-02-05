# UUID Generation Benchmarks: clj-uuid-old vs clj-uuid

Performance comparison of UUID generation across all RFC 9562 versions,
measuring `clj-uuid-old` (bitmop, shift/mask loops) against `clj-uuid`
(bitmop2, ByteBuffer primitives + JVM intrinsic mask operations +
ThreadLocal MessageDigest).

## Test Environment

### Hardware

- **CPU:** Intel Core i9-9880H @ 2.30 GHz (8 cores / 16 threads)
- **RAM:** 32 GB
- **Architecture:** x86_64

### Software

- **OS:** macOS 26.2 (Darwin 25.2.0)
- **JVM:** OpenJDK 64-Bit Server VM 25.0.1 (Homebrew, mixed mode, sharing)
- **Clojure:** 1.12.0
- **Leiningen:** 2.12.0

### Benchmark Parameters

- **Iterations:** 500,000 per benchmark
- **Warmup:** 50,000 iterations (JIT compilation)
- **Reflection warnings:** none (verified via `lein check` and
  `*warn-on-reflection*`)
- **Source:** `test/clj_uuid/bench.clj`

## 1. UUID Generation (Pure Construction)

Measures only the time to call the constructor and return a
`java.util.UUID` value.  No serialization.

| UUID Version             | clj-uuid-old (ns) | clj-uuid (ns) | Speedup |
|--------------------------|-------------------:|---------------:|--------:|
| v1 (time-based)          |            120.3   |        100.1   |  1.20x  |
| v3 (MD5, namespace)      |           1409.1   |        165.9   |  8.49x  |
| v4 (random)              |            326.3   |        334.8   |  0.97x  |
| v5 (SHA1, namespace)     |           1531.4   |        264.8   |  5.78x  |
| v6 (time-based, sorted)  |            106.3   |        100.0   |  1.06x  |
| v7 (unix time, crypto)   |            408.8   |        340.0   |  1.20x  |
| v7nc (unix time, fast)   |               --   |         38.5   |    --   |
| v8 (custom)              |             46.4   |          7.9   |  5.87x  |

### Analysis

**v3 and v5 show 5-9x generation speedup.**  These are the only versions
where the optimized implementation changes the generation path itself.
Three optimizations compound:

1. **ThreadLocal MessageDigest:** `clj-uuid` reuses a per-thread
   `MessageDigest` instance via `ThreadLocal/withInitial`, avoiding the
   ~200 ns `MessageDigest/getInstance` allocation on every call.
   `clj-uuid-old` also uses ThreadLocal (both were optimized in this pass).
2. **Byte serialization:** Serialize the namespace UUID via `to-byte-array`
   (bitmop: 2x 8-iteration `ldb`+`sb8` loops; bitmop2: 2x
   `ByteBuffer.putLong`).
3. **Digest extraction:** Read back the digest result via `bytes->long`
   (bitmop: 2x 8-iteration `dpb` loops; bitmop2: 2x `ByteBuffer.getLong`).

In `clj-uuid-old`, byte manipulation overhead adds ~1200 ns on top of the
~200 ns digest.  In `clj-uuid`, that overhead is nearly eliminated, leaving
the digest as the dominant cost.

**v7 shows 1.21x speedup** from `mask-offset` optimization.  The v7
constructor calls `dpb #=(mask 2 62)` to set the variant bits in the LSB.
The `#=(mask 2 62)` is a compile-time constant, but `dpb` calls
`mask-offset` at runtime to find the lowest set bit.  Previously,
`mask-offset` used an O(offset) loop -- for offset=62, that meant 62
iterations per call.  Now `mask-offset` uses `Long/numberOfTrailingZeros`,
a JVM intrinsic that compiles to a single `TZCNT` instruction.  This
eliminates the v7 regression that was visible in earlier benchmarks.
`SecureRandom.nextLong()` still dominates total latency.

**v8 shows 4.21x speedup** from `mask-offset` optimization.  The v8
constructor is just two `dpb` calls (`mask(4,12)` and `mask(2,62)`), so
the `mask-offset` cost was a significant fraction of the total.  With O(1)
`Long/numberOfTrailingZeros`, the two `dpb` calls drop from ~46 ns to
~11 ns.

**v1 shows ~1.2x and v6 shows ~1.1x** relative to clj-uuid-old.  In addition
to the O(1) `mask-offset` improvement, v1 and v6 now inline the `AtomicLong`
CAS loop, bit-field packing, and pre-captured node LSBs directly in the
constructor closure, eliminating var lookup and function dispatch overhead.
The `System/currentTimeMillis` + CAS cost remains the dominant factor.

**v7nc is new in 0.2.5** and has no clj-uuid-old equivalent.  It uses
`ThreadLocalRandom` and a per-thread monotonic counter, achieving
38.5 ns/op -- faster than JUG 5.2's `TimeBasedEpochGenerator` (~50 ns).

**v4 shows ~1x.**  The 0-arity form delegates directly to
`UUID/randomUUID` (JVM built-in, dominated by SecureRandom).

## 2. Post-Generation Operations

Measures operations on a pre-existing UUID value.  These are the operations
where bitmop2's ByteBuffer approach has the most impact.

| Operation      | clj-uuid-old (ns) | clj-uuid (ns) | Speedup |
|----------------|-------------------:|---------------:|--------:|
| to-byte-array  |            803.6   |         14.0   | 57.40x  |
| to-hex-string  |           5840.4   |        126.1   | 46.32x  |
| to-string      |             22.1   |         17.6   |  1.26x  |
| to-urn-string  |            110.1   |        110.9   |  0.99x  |
| get-version    |              7.1   |          7.5   |  0.95x  |
| get-node-id    |             11.9   |          9.8   |  1.21x  |

### Analysis

**`to-byte-array`: 57x faster.**  This is the biggest win.  bitmop requires
two 8-iteration loops (each doing `ldb` + `sb8` per byte, 16 iterations
total).  bitmop2 does two `ByteBuffer.putLong` calls -- single JVM
intrinsics.

**`to-hex-string`: 29x faster.**  bitmop builds two separate hex strings
via `(hex msb)` and `(hex lsb)`, each involving `long->bytes` (8-iteration
loop), `map ub8`, `map octet-hex`, and `apply str` (lazy sequence
materialization + string concatenation).  bitmop2 uses `uuid->buf` +
`buf-hex`: a single `StringBuilder` with direct byte iteration over a
`ByteBuffer`.

**`to-string` and `to-urn-string`: ~1x (no change).**  Both delegate to
`UUID.toString()`, a JVM-native method that neither bitmop touches.

**Field extraction (`get-version`, `get-node-id`): ~1x.**  These use
`ldb`/`dpb` on the UUID's long words, which are identical between the
two implementations.  The slight variation is measurement noise.

## 3. Combined: Generate + Serialize

The real-world pattern -- generate a UUID and immediately serialize it for
storage, transmission, or indexing.

| Operation              | clj-uuid-old (ns) | clj-uuid (ns) | Speedup |
|------------------------|-------------------:|---------------:|--------:|
| v1 + to-byte-array     |            925.9   |        112.8   |  8.21x  |
| v3 + to-byte-array     |           2223.6   |        157.1   | 14.15x  |
| v3 + to-hex-string     |           6425.8   |        297.8   | 21.58x  |
| v4 + to-byte-array     |           1170.8   |        350.4   |  3.34x  |
| v4 + to-hex-string     |           5366.4   |        476.8   | 11.25x  |
| v5 + to-byte-array     |           2327.4   |        269.0   |  8.65x  |
| v5 + to-hex-string     |           6509.5   |        409.0   | 15.92x  |
| v7 + to-byte-array     |           1303.1   |        324.8   |  4.01x  |

### Analysis

The combined numbers reflect the sum of generation and serialization gains.

**v3 + to-hex-string: 18.9x.**  This is the largest combined win.  v3
benefits from faster generation (9x from ThreadLocal + `bytes->long` and
`to-byte-array` in the digest path) AND faster serialization (29x from the
hex output path).  The two effects compound.

**v5 + to-hex-string: 14.6x.**  Same compounding effect as v3, but SHA-1
is slightly slower than MD5, so the digest fraction is larger and the byte
manipulation speedup contributes proportionally less.

**v3 + to-byte-array: 14.1x / v5 + to-byte-array: 8.6x.**  Byte-array
serialization is faster than hex (57x vs 29x), but takes less absolute
time, so the generation speedup contributes more to the total ratio.

**v1 + to-byte-array: 8.0x.**  Generation is ~1.2x but serialization is 57x.
The serialization dominates total time in clj-uuid-old (~87% of 926 ns) but
becomes negligible in clj-uuid (~14 ns of 115 ns).

**v4 + to-byte-array: 3.4x.**  `UUID/randomUUID` is the bottleneck (~340
ns), so the serialization savings (804 ns -> 14 ns) yield a 3.4x total win.

**v4 + to-hex-string: 9.9x.**  The hex path has even more overhead in
clj-uuid-old (~5840 ns) so the combined win is larger than the byte-array
case.

**v7 + to-byte-array: 3.9x.**  Similar profile to v4 -- crypto RNG
dominates, but the O(1) mask-offset now contributes a generation speedup
on top of the serialization win.

## 4. Absolute Throughput

UUIDs generated per second (generation only, single thread).

| UUID Version             | clj-uuid-old (ops/s) | clj-uuid (ops/s) |
|--------------------------|---------------------:|------------------:|
| v1 (time-based)          |          6,353,878   |       9,385,662   |
| v3 (MD5, namespace)      |            668,818   |       3,239,594   |
| v4 (random)              |          2,715,643   |       2,680,676   |
| v5 (SHA1, namespace)     |            648,846   |       3,024,212   |
| v6 (time-based, sorted)  |          8,569,979   |      10,024,839   |
| v7 (unix time, crypto)   |          2,492,207   |       2,960,438   |
| v7nc (unix time, fast)   |                  --  |      25,974,026   |
| v8 (custom)              |         19,381,529   |     149,993,940   |

## 5. v3/v5 Detailed Breakdown

Since v3 and v5 are the UUID types with the largest generation-time
improvements, this section breaks down the per-operation costs.

### v3 (MD5, Namespace)

| Operation        | clj-uuid-old (ns) | clj-uuid (ns) | Speedup |
|------------------|-------------------:|---------------:|--------:|
| v3 generation    |           1412.0   |        156.9   |  9.00x  |
| v3 to-byte-array |            804.7   |         13.7   | 58.81x  |
| v3 to-hex-string |           5225.1   |        197.7   | 26.43x  |
| v3 to-string     |             23.7   |         24.3   |  0.98x  |

### v5 (SHA1, Namespace)

| Operation        | clj-uuid-old (ns) | clj-uuid (ns) | Speedup |
|------------------|-------------------:|---------------:|--------:|
| v5 generation    |           1667.6   |        279.0   |  5.98x  |
| v5 to-byte-array |            849.0   |         16.4   | 51.71x  |
| v5 to-hex-string |           5128.9   |        185.4   | 27.66x  |
| v5 to-string     |             22.9   |         22.0   |  1.04x  |

### v3/v5 Generation Path Breakdown

The v3/v5 generation path consists of four steps.  The following shows
where time is spent in each implementation:

```
                      clj-uuid-old               clj-uuid
  to-byte-array:      ~800 ns (2x long->bytes    ~14 ns (2x putLong)
                        8-iter ldb+sb8 loop)
  digest (MD5/SHA1):  ~200-300 ns                ~200-300 ns
  bytes->long:        ~800 ns (2x 8-iter dpb)    ~14 ns (2x getLong)
  dpb:                ~5 ns (2 calls)            ~3 ns (2 calls, O(1) mask-offset)
  ────────────────────────────────────────────────────────
  Total (v3):         ~1400 ns                   ~160 ns
  Total (v5):         ~1670 ns                   ~280 ns
```

In `clj-uuid-old`, byte manipulation overhead (~1600 ns) dominates over the
digest (~200-300 ns).  In `clj-uuid`, byte manipulation is eliminated
(~28 ns total), leaving the digest as the dominant cost.

## 6. Summary

### Where clj-uuid wins

| Category                           | Speedup    |
|------------------------------------|------------|
| `to-byte-array`                    | **57x**    |
| `to-hex-string`                    | **29x**    |
| v3 + to-hex-string (combined)      | **18.9x**  |
| v5 + to-hex-string (combined)      | **14.6x**  |
| v3 + to-byte-array (combined)      | **14.1x**  |
| v4 + to-hex-string (combined)      | **9.9x**   |
| v3 generation                      | **9.0x**   |
| v5 + to-byte-array (combined)      | **8.6x**   |
| v1 + to-byte-array (combined)      | **8.0x**   |
| v5 generation                      | **6.0x**   |
| v8 generation                      | **4.2x**   |
| v7 + to-byte-array (combined)      | **3.9x**   |
| v4 + to-byte-array (combined)      | **3.4x**   |
| v1 generation                      | **1.5x**   |
| v6 generation                      | **1.4x**   |
| v7 generation                      | **1.3x**   |

### Where they are equal

| Category                           | Speedup    |
|------------------------------------|------------|
| v4 generation (0-arity)            | ~1.0x      |
| `to-string` / `to-urn-string`     | ~1.0x      |
| Field extraction (version, node)   | ~1.0x      |

### Where clj-uuid has no impact

Operations that delegate entirely to the JVM (`UUID.toString()`,
`UUID/randomUUID`, `UUID.version()`) see no change, as expected.
The bitmop2 layer only affects byte-level serialization, the
`bytes->long` / `long->bytes` paths, and `ldb`/`dpb` calls (which
now benefit from O(1) `mask-offset` via `Long/numberOfTrailingZeros`).

### New in 0.2.5: v7nc

`v7nc` is a non-cryptographic v7 variant that uses `ThreadLocalRandom`
and a per-thread monotonic counter.  At 38.5 ns/op it is faster than
JUG 5.2's `TimeBasedEpochGenerator` (~50 ns), making it the fastest
time-based UUID generator available on the JVM from Clojure.

### Key Takeaway

The largest gains appear in **serialization** (`to-byte-array`,
`to-hex-string`) and in **v3/v5 generation** (which serialize the
namespace UUID internally as part of the digest computation).  Additional
gains come from **O(1) mask-offset** using JVM intrinsics, which
particularly benefits v8 (4.2x) where `dpb` calls with high-offset
masks were previously bottlenecked by an O(offset) loop.  The new
`v7nc` constructor provides the fastest time-based UUID generation
at ~39 ns, beating JUG 5.2.  For applications that generate UUIDs
and immediately serialize them -- the common case for database keys,
wire protocols, and log correlation IDs -- clj-uuid delivers
**3-19x end-to-end improvement** depending on the UUID version and
serialization format.
