# UUID Generation Benchmarks: clj-uuid-old vs clj-uuid

Performance comparison of UUID generation across all RFC 9562 versions,
measuring `clj-uuid-old` (bitmop, shift/mask loops) against `clj-uuid`
(bitmop2, ByteBuffer primitives + ThreadLocal MessageDigest).

## Test Environment

- **JVM:** OpenJDK 64-Bit Server VM, Java 25.0.1
- **Iterations:** 500,000 per benchmark
- **Warmup:** 50,000 iterations (JIT compilation)
- **Platform:** macOS (Darwin 25.2.0), Apple Silicon
- **Reflection warnings:** none (verified via `lein check` and
  `*warn-on-reflection*`)
- **Benchmark source:** `test/clj_uuid/bench.clj`

## 1. UUID Generation (Pure Construction)

Measures only the time to call the constructor and return a
`java.util.UUID` value.  No serialization.

| UUID Version             | clj-uuid-old (ns) | clj-uuid (ns) | Speedup |
|--------------------------|-------------------:|---------------:|--------:|
| v1 (time-based)          |            126.0   |        105.3   |  1.20x  |
| v3 (MD5, namespace)      |           1482.2   |        201.3   |  7.36x  |
| v4 (random)              |            339.5   |        344.5   |  0.99x  |
| v5 (SHA1, namespace)     |           1617.9   |        308.5   |  5.25x  |
| v6 (time-based, sorted)  |            101.4   |        103.9   |  0.98x  |
| v7 (unix time, crypto)   |            389.1   |        419.6   |  0.93x  |
| v8 (custom)              |             48.1   |         37.2   |  1.29x  |

### Analysis

**v3 and v5 show 5-7x generation speedup.**  These are the only versions
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

**v1, v4, v6, v7, v8 show ~1x.**  These versions construct the UUID
entirely from long arithmetic (`ldb`/`dpb` on longs), which is identical
between bitmop and bitmop2 -- both compile to the same shift/mask JVM
bytecode.  The bottleneck in each case is external to bitmop:

- v1/v6: monotonic clock (CAS contention)
- v4: `UUID/randomUUID` (CSPRNG)
- v7: monotonic unix clock + `SecureRandom`
- v8: two `dpb` calls (pure arithmetic, ~40 ns)

## 2. Post-Generation Operations

Measures operations on a pre-existing UUID value.  These are the operations
where bitmop2's ByteBuffer approach has the most impact.

| Operation      | clj-uuid-old (ns) | clj-uuid (ns) | Speedup |
|----------------|-------------------:|---------------:|--------:|
| to-byte-array  |            811.2   |         13.6   | 60.09x  |
| to-hex-string  |           5116.0   |        133.3   | 38.44x  |
| to-string      |             19.4   |         19.8   |  0.98x  |
| to-urn-string  |             95.9   |         85.8   |  1.12x  |
| get-version    |              8.2   |          9.8   |  0.84x  |
| get-node-id    |             10.0   |         11.1   |  0.90x  |

### Analysis

**`to-byte-array`: 60x faster.**  This is the biggest win.  bitmop requires
two 8-iteration loops (each doing `ldb` + `sb8` per byte, 16 iterations
total).  bitmop2 does two `ByteBuffer.putLong` calls -- single JVM
intrinsics.

**`to-hex-string`: 38x faster.**  bitmop builds two separate hex strings
via `(hex msb)` and `(hex lsb)`, each involving `long->bytes` (8-iteration
loop), `map ub8`, `map octet-hex`, and `apply str` (lazy sequence
materialization + string concatenation).  bitmop2 uses `uuid->buf` +
`buf-hex`: a single `StringBuilder` with direct byte iteration over a
`ByteBuffer`.

**`to-string` and `to-urn-string`: ~1x (no change).**  Both delegate to
`UUID.toString()`, a JVM-native method that neither bitmop touches.

**Field extraction (`get-version`, `get-node-id`): ~1x.**  These use
`ldb`/`dpb` on the UUID's long words, which are identical between the
two implementations.

## 3. Combined: Generate + Serialize

The real-world pattern -- generate a UUID and immediately serialize it for
storage, transmission, or indexing.

| Operation              | clj-uuid-old (ns) | clj-uuid (ns) | Speedup |
|------------------------|-------------------:|---------------:|--------:|
| v1 + to-byte-array     |            948.5   |        104.8   |  9.05x  |
| v3 + to-byte-array     |           2301.8   |        204.5   | 11.28x  |
| v3 + to-hex-string     |           6783.4   |        334.6   | 20.30x  |
| v4 + to-byte-array     |           1246.1   |        351.6   |  3.55x  |
| v4 + to-hex-string     |           5541.8   |        498.3   | 11.09x  |
| v5 + to-byte-array     |           2479.7   |        314.9   |  7.88x  |
| v5 + to-hex-string     |           6594.6   |        452.9   | 14.56x  |
| v7 + to-byte-array     |           1256.5   |        421.5   |  3.01x  |

### Analysis

The combined numbers reflect the sum of generation and serialization gains.

**v3 + to-hex-string: 20x.**  This is the largest combined win.  v3
benefits from faster generation (7x from ThreadLocal + `bytes->long` and
`to-byte-array` in the digest path) AND faster serialization (38x from the
hex output path).  The two effects compound.

**v5 + to-hex-string: 14.6x.**  Same compounding effect as v3, but SHA-1
is slightly slower than MD5, so the digest fraction is larger and the byte
manipulation speedup contributes proportionally less.

**v3 + to-byte-array: 11.3x / v5 + to-byte-array: 7.9x.**  Byte-array
serialization is faster than hex (60x vs 38x), but takes less absolute
time, so the generation speedup contributes more to the total ratio.

**v1 + to-byte-array: 9.1x.**  Generation is ~1x but serialization is 60x.
The serialization dominates total time in clj-uuid-old (~85% of 949 ns) but
becomes negligible in clj-uuid (~14 ns of 105 ns).

**v4 + to-byte-array: 3.6x.**  `UUID/randomUUID` is the bottleneck (~345
ns), so the serialization savings (811 ns -> 14 ns) yield a 3.6x total win.

**v4 + to-hex-string: 11.1x.**  The hex path has even more overhead in
clj-uuid-old (~5100 ns) so the combined win is larger than the byte-array
case.

**v7 + to-byte-array: 3.0x.**  Similar profile to v4 -- crypto RNG
dominates.

## 4. Absolute Throughput

UUIDs generated per second (generation only, single thread).

| UUID Version             | clj-uuid-old (ops/s) | clj-uuid (ops/s) |
|--------------------------|---------------------:|------------------:|
| v1 (time-based)          |          7,222,723   |       6,172,999   |
| v3 (MD5, namespace)      |            658,464   |       4,122,833   |
| v4 (random)              |          2,385,527   |       2,891,178   |
| v5 (SHA1, namespace)     |            635,385   |       2,749,038   |
| v6 (time-based, sorted)  |          7,781,531   |       8,544,691   |
| v7 (unix time, crypto)   |          2,566,094   |       2,436,149   |
| v8 (custom)              |         16,970,016   |      26,803,579   |

## 5. v3/v5 Detailed Breakdown

Since v3 and v5 are the UUID types with the largest generation-time
improvements, this section breaks down the per-operation costs.

### v3 (MD5, Namespace)

| Operation        | clj-uuid-old (ns) | clj-uuid (ns) | Speedup |
|------------------|-------------------:|---------------:|--------:|
| v3 generation    |           1449.2   |        197.8   |  7.34x  |
| v3 to-byte-array |            806.5   |         14.5   | 55.62x  |
| v3 to-hex-string |           5140.4   |        128.8   | 39.94x  |
| v3 to-string     |             18.2   |         21.0   |  0.87x  |

### v5 (SHA1, Namespace)

| Operation        | clj-uuid-old (ns) | clj-uuid (ns) | Speedup |
|------------------|-------------------:|---------------:|--------:|
| v5 generation    |           1599.9   |        303.8   |  5.27x  |
| v5 to-byte-array |            823.4   |         15.2   | 54.28x  |
| v5 to-hex-string |           5137.4   |        131.9   | 38.96x  |
| v5 to-string     |             19.8   |         21.5   |  0.92x  |

### v3/v5 Generation Path Breakdown

The v3/v5 generation path consists of four steps.  The following shows
where time is spent in each implementation:

```
                      clj-uuid-old               clj-uuid
  to-byte-array:      ~800 ns (2x long->bytes    ~14 ns (2x putLong)
                        8-iter ldb+sb8 loop)
  digest (MD5/SHA1):  ~200-300 ns                ~200-300 ns
  bytes->long:        ~800 ns (2x 8-iter dpb)    ~14 ns (2x getLong)
  dpb:                ~5 ns (2 calls)            ~5 ns (2 calls)
  ────────────────────────────────────────────────────────
  Total (v3):         ~1450 ns                   ~200 ns
  Total (v5):         ~1600 ns                   ~300 ns
```

In `clj-uuid-old`, byte manipulation overhead (~1600 ns) dominates over the
digest (~200-300 ns).  In `clj-uuid`, byte manipulation is eliminated
(~28 ns total), leaving the digest as the dominant cost.

## 6. Summary

### Where clj-uuid wins

| Category                           | Speedup    |
|------------------------------------|------------|
| `to-byte-array`                    | **60x**    |
| `to-hex-string`                    | **38x**    |
| v3 + to-hex-string (combined)      | **20.3x**  |
| v5 + to-hex-string (combined)      | **14.6x**  |
| v3 + to-byte-array (combined)      | **11.3x**  |
| v4 + to-hex-string (combined)      | **11.1x**  |
| v1 + to-byte-array (combined)      | **9.1x**   |
| v5 + to-byte-array (combined)      | **7.9x**   |
| v3 generation                      | **7.4x**   |
| v5 generation                      | **5.3x**   |

### Where they are equal

| Category                           | Speedup    |
|------------------------------------|------------|
| v1, v4, v6, v7, v8 generation     | ~1.0x      |
| `to-string` / `to-urn-string`     | ~1.0x      |
| Field extraction (version, node)   | ~1.0x      |

### Where clj-uuid has no impact

Operations that delegate entirely to the JVM (`UUID.toString()`,
`UUID/randomUUID`, `UUID.version()`) see no change, as expected.
The bitmop2 layer only affects byte-level serialization and the
`bytes->long` / `long->bytes` paths.

### Key Takeaway

The largest gains appear in **serialization** (`to-byte-array`,
`to-hex-string`) and in **v3/v5 generation** (which serialize the
namespace UUID internally as part of the digest computation).  For
applications that generate UUIDs and immediately serialize them -- the
common case for database keys, wire protocols, and log correlation IDs --
clj-uuid delivers **3-20x end-to-end improvement** depending on the
UUID version and serialization format.
