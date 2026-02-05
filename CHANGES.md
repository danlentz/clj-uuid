# Changes

## 0.2.5

### Performance

The bitmop layer has been rewritten around ByteBuffer-based primitives
and JVM intrinsics, delivering substantial performance gains across
the board with no changes to the public API.

`mask-offset`, `mask-width`, and `bit-count` now compile to single
hardware instructions (`TZCNT`, `POPCNT`) via `Long/numberOfTrailingZeros`
and `Long/bitCount`, replacing the previous O(n) loops.

A new ByteBuffer abstraction (`uuid->buf`, `buf->uuid`) provides
direct typed access at byte offsets via native `getLong`/`putLong`
operations, eliminating manual shift/mask loops for serialization.

Representative speedups over 0.2.0:

| Category                        | Speedup     |
|---------------------------------|-------------|
| `to-byte-array`                 | **57x**     |
| `to-hex-string`                 | **29x**     |
| v3 (MD5) generation             | **9.0x**    |
| v5 (SHA1) generation            | **6.0x**    |
| v8 (custom) generation          | **4.2x**    |
| v1 (time-based) generation      | **1.5x**    |
| v6 (time-based) generation      | **1.4x**    |

Combined generate + serialize operations see 3-19x end-to-end
improvement depending on UUID version and serialization format.

v3/v5 generation now uses a fused digest pipeline with ThreadLocal
ByteBuffer reuse, `ByteBuffer/wrap` on digest output, and inlined
version/variant bit stamping -- eliminating all intermediate
allocations and var lookups.  v5 is now at parity with JUG 5.2
(~260 ns vs ~254 ns).

### Correctness

- `get-clk-seq` now uses `bitmop/ldb` to extract the 14-bit clock
  sequence directly, fixing incorrect results that occurred when
  Java's `.clockSequence()` method threw on non-v1 UUIDs.

### Clock improvements

- The Gregorian monotonic clock now uses `AtomicLong` with
  `compareAndSet` on a packed long (50-bit millis + 14-bit seqid),
  replacing `atom` + `swap!` + per-call `State` allocation.
- v1 and v6 constructors inline the CAS loop and bit-field packing,
  eliminating var lookup, function dispatch, and `ldb`/`dpb` overhead
  on the hot path.
- The monotonic counter initial state is now seeded with a
  cryptographically random 10-bit value instead of zero, avoiding
  predictable first values after library load.
- Per-tick counter reseed uses 10-bit entropy (was 8-bit).

### New UUID versions

- v6 (reordered time-based, lexically sortable)
- v7 (unix time-based, cryptographically secure, lexically sortable)
- v7nc (unix time-based, non-cryptographic, maximum throughput)
- v8 (custom / user-defined)
- Max UUID sentinel (`+max+`, `max`, `max?`)

`v7nc` uses `ThreadLocalRandom` and a per-thread monotonic counter
instead of `SecureRandom` and a global `AtomicLong`.  At ~39 ns/op,
it is **1.26x faster** than JUG 5.2's `TimeBasedEpochGenerator`
(~50 ns/op).

### New protocol members

- `get-instant` -- returns a `java.util.Date` for time-based UUIDs
- `get-unix-time` -- returns POSIX millis for v1, v6, and v7
- `get-clk-seq` -- returns the 14-bit clock sequence for v1 and v6
- `to-hex-string` -- 32-character hex encoding (no dashes)
- `to-uri` -- returns `java.net.URI` in URN format
- `max?` -- predicate for the max UUID
- `UUIDRfc9562` protocol (with `UUIDRfc4122` as a backward-compatible alias)

### New constructors

- `v0` / `null` -- null UUID constructor
- `max` -- max UUID constructor
- `v4` two-arity form `(v4 msb lsb)` -- stamps version and variant bits onto
  caller-supplied words
- Multi-arity `=`, `<`, `>` comparison operators

### Extended polymorphism

- `as-uuid` now accepts `java.net.URI`, byte arrays, and URN strings
  in addition to canonical UUID strings
- `uuidable?` predicate for testing coercibility

### Build and packaging

- Added `deps.edn` for tools.deps / CLI users
- Removed `.travis.yml`; CI now uses GitHub Actions
- Updated to Clojure 1.12.0 and primitive-math 1.0.1
- Comprehensive test suite: 138 tests, 1.4M+ assertions, 0 failures
- Test coverage: 94% forms, 93% lines (via cloverage)

### Documentation

- README rewritten to cover v6, v7, v8, max UUID, and the new
  performance characteristics
- Typo corrections throughout (`get-timestamp`, `approximately`,
  `associated`, `requirements`, `consciousness`, `alphabetically`,
  `elapsed`, `duplicate`, `construction`)
