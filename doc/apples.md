# Comparative Benchmarks: clj-uuid vs JUG vs uuid-creator vs JDK

Apples-to-apples performance comparison of UUID generation across
four JVM implementations, measured on the same machine in the same
JVM process.

## Libraries

| Library | Version | Coordinates |
|---|---|---|
| **clj-uuid** | 0.2.5-SNAPSHOT | `danlentz/clj-uuid` |
| **JUG** (FasterXML) | 5.2.0 | `com.fasterxml.uuid/java-uuid-generator` |
| **uuid-creator** (f4b6a3) | 6.1.1 | `com.github.f4b6a3/uuid-creator` |
| **JDK** | OpenJDK 25.0.1 | `java.util.UUID` (built-in) |

## Environment

- **CPU:** Intel Core i9-9880H @ 2.30 GHz (8 cores / 16 threads)
- **RAM:** 32 GB
- **OS:** macOS (Darwin 25.2.0, x86_64)
- **JVM:** OpenJDK 25.0.1 (Homebrew, mixed mode, sharing)
- **Clojure:** 1.12.0

## Method

500,000 iterations per benchmark after a 50,000 iteration JIT warmup.
Each cell is the average ns/op.  Source: `test/clj_uuid/compare_bench.clj`.

## Results

| Operation          | clj-uuid (ns) | JUG 5.2 (ns) | uuid-creator (ns) | JDK (ns) |
|--------------------|---------------:|--------------:|-------------------:|---------:|
| v1 (time-based)    |          100.1 |          58.6 |               72.1 |       -- |
| v4 (random)        |          340.9 |         340.9 |              369.1 |    338.1 |
| v5 (SHA1)          |          260.5 |         253.8 |              301.9 |       -- |
| v6 (time-ordered)  |          100.0 |          46.4 |               54.9 |       -- |
| v7 (unix epoch)    |          333.2 |          51.1 |              272.2 |       -- |
| v7nc (fast epoch)  |           39.4 |          49.6 |                 -- |       -- |
| to-string          |           18.8 |            -- |                 -- |     14.4 |
| to-byte-array      |           13.7 |            -- |                 -- |       -- |

## Analysis

### v4 (random)

All four implementations cluster between 338-370 ns.  The dominant
cost is `SecureRandom.nextLong()`, which is common to all of them.
clj-uuid delegates directly to `java.util.UUID/randomUUID` and is
effectively at parity with the JDK.

### v1 (time-based)

clj-uuid is **1.7x** slower than JUG.  JUG uses raw `System.currentTimeMillis()`
with an internal synchronized counter.  clj-uuid uses an `AtomicLong`
CAS-based monotonic clock that computes a Gregorian-epoch 100 ns
timestamp with inlined bit-field packing.  The remaining gap is
Clojure `defn` dispatch overhead and Gregorian epoch arithmetic.

### v5 (SHA1, name-based)

clj-uuid is **1.03x** slower than JUG -- effectively at parity.
Both use `ThreadLocal<MessageDigest>`.  The fused ByteBuffer
implementation eliminated intermediate allocations and var lookups
in the digest pipeline, closing the gap that previously existed.

### v6 (time-ordered)

clj-uuid is **2.2x** slower than JUG.  Same clock as v1, with different
bit-field ordering for lexical sorting.  The gap is wider than v1 because
JUG's v6 is exceptionally fast (46 ns), leaving Clojure `defn` dispatch
and Gregorian arithmetic as a proportionally larger overhead.

### v7 (unix epoch, CSPRNG)

clj-uuid `v7` is **6.5x** slower than JUG.  This reflects a **design
choice, not a deficiency**.  The v7 UUID layout includes a 62-bit
random field (`rand_b`).  The libraries differ in how they fill it:

| Library | rand_b source | Cost |
|---|---|---|
| **JUG** | Monotonic counter (no per-call randomness) | ~0 ns |
| **uuid-creator** | `SecureRandom` | ~280 ns |
| **clj-uuid v7** | `SecureRandom` | ~280 ns |

RFC 9562 Section 6.9 ("Unguessability") recommends that
implementations "utilize a cryptographically secure pseudorandom
number generator (CSPRNG) to provide values that are both difficult
to predict (unguessable) and have a low likelihood of collision."

clj-uuid `v7` and uuid-creator follow the RFC recommendation by
calling `SecureRandom` on every generation, paying ~280 ns for
the CSPRNG call.

### v7nc (fast epoch, ThreadLocalRandom) -- fastest v7

clj-uuid `v7nc` is **1.26x faster** than JUG (39.4 ns vs 49.6 ns).

`v7nc` uses `ThreadLocalRandom` instead of `SecureRandom` and a
per-thread monotonic counter instead of a global `AtomicLong`.
The per-thread design eliminates CAS contention entirely, and the
hot path (same millisecond) requires no random number generation --
just an increment and a UUID constructor call.

This is the appropriate choice for applications that prioritize
throughput over cryptographic unguessability of the random portion.

### to-string

clj-uuid calls `java.util.UUID.toString()` directly.  The ~4 ns
overhead versus the JDK measurement is Clojure protocol dispatch.

## Summary

clj-uuid is at parity with JUG for v4 (random) and v5 (SHA1)
generation, and **faster** than JUG for non-cryptographic v7
generation via `v7nc`.  JUG is faster for time-based v1 and v6
generation due to pure-Java code paths with no Clojure dispatch
overhead.

| Category | vs JUG |
|---|---|
| v7nc (fast epoch) | **1.26x faster** |
| v4 (random) | ~1.0x (parity) |
| v5 (SHA1) | ~1.0x (parity) |
| v1 (time-based) | 1.7x slower |
| v6 (time-ordered) | 2.2x slower |
| v7 (CSPRNG) | 6.5x slower (by design) |

clj-uuid provides both `v7` (CSPRNG, RFC 6.9 compliant) and `v7nc`
(ThreadLocalRandom, maximum throughput), letting applications choose
the appropriate tradeoff.
