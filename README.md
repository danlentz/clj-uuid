# clj-uuid

[![Build Status](https://travis-ci.org/danlentz/clj-uuid.svg?branch=master)]
(https://travis-ci.org/danlentz/clj-uuid)
[![Dependency Status](https://www.versioneye.com/clojure/danlentz:clj-uuid/0.1.0-SNAPSHOT/badge.svg)](https://www.versioneye.com/clojure/danlentz:clj-uuid/0.1.0-SNAPSHOT)
[![Reference Status](https://www.versioneye.com/clojure/danlentz:clj-uuid/reference_badge.svg?style=flat)](https://www.versioneye.com/clojure/danlentz:clj-uuid/references)

A Clojure library for generation and utilization of UUIDs (Universally
Unique Identifiers) as described by RFC-4122. This library extends the
standard Java UUID class to provide true v1 (time based) and v3/v5
(namespace based) identifier generation. Additionally, a number of
useful supporting utilities are provided to support serialization and
manipulation of these UUIDs in a simple, efficient manner.

The essential nature of the value RFC4122 UUIDs provide is that of an
enormous namespace and a deterministic mathematical model by means of
which one navigates it. UUIDs represent an extremely powerful and
versatile computation technique that is often overlooked, and
underutilized. In my opinion, this, in part, is due to the generally
poor quality, performance, and capability of available libraries and,
in part, due to a general misunderstanding in the popular consiousness
of their proper use and benefit. It is my hope that this library will
serve to expand awareness, make available, and simplify use of RFC4122
identifiers to a wider audience.

## The Most Recent Release

With Leiningen:

![Clojars Project](http://clojars.org/danlentz/clj-uuid/latest-version.svg)


## How is it better?

The JVM version only provides an automatic builder for random (v4)
and (non-namespaced) pseudo-v3 UUID's.  Where appropriate, this library
does use the internal JVM UUID implementation.  The benefit with this library
is that clj-uuid provides an easy way to get v1 and true namespaced v3 and
v5 UUIDs.  v3/v5 UUID's are necessary because many of the interesting things
that you can do with UUID's require namespaced identifiers. v1 UUIDs are
really useful because they can be generated faster than v4's as they don't
need to call a cryptographic random number generator.


## How Big?

The provided namespace represents an _inexhaustable_ resource and as
such can be used in a variety of ways not feasible using traditional
techniques rooted in the notions imposed by finite resources.  When I
say "inexhaustable" this of course is slight hyperbolie, but not by
much.  The upper bound on the representation implemented by this
library limits the number of unique identifiers to a mere...

*three hundred forty undecillion two hundred eighty-two decillion three*
*hundred sixty-six nonillion nine hundred twenty octillion nine hundred* 
*thirty-eight septillion four hundred sixty-three sextillion four hundred*
*sixty-three quintillion three hundred seventy-four quadrillion six hundred*
*seven trillion four hundred thirty-one billion seven hundred sixty-eight*
*million two hundred eleven thousand four hundred and fifty-five.*

If you think you might be starting to run low, let me know when you get down
to your last few undecillion or so and I'll see what I can do to help out.


## Usage

Using clj-uuid is really easy.  Docstrings are provided, but sometimes
examples help, too.  The following cases demonstrate about 90% of the
functionality that you are likely to ever need.

In order to refer to the symbols in this library, it is recommended to
*require* it in a given namespace:

```clojure

(require '[clj-uuid :as uuid])
```

Or include in namespace declaration:


```clojure

(ns foo
  (:require [clj-uuid :as uuid])
  ...
  )

```

### Literal Syntax

UUID's have a convenient literal syntax supported by the clojure
reader.  The tag `#uuid` denotes that the following string literal
will be read as a UUID.  UUID's evaluate to themselves:

```clojure

user> #uuid "e6ff478d-9492-48dd-886d-23ec4c6385ee"

;;  => #uuid "e6ff478d-9492-48dd-886d-23ec4c6385ee"
```

### The NULL (v0) Identifier


### Time Based (v1) Identifiers

You can make your own v1 UUID's with the function `#'uuid/v1`.  These
UUID's will be guaranteed to be unique and thread-safe regardless of
clock precision or degree of concurrency.

A v1 UUID may reveal both the identity of the computer that generated
the UUID and the time at which it did so.  Its uniqueness across
computers is guaranteed as long as node/MAC addresses are not duplicated.
  

```clojure

user> (uuid/v1)

;;  => #uuid "ffa803f0-b3d3-11e4-a03e-3af93c3de9ae"

user> (uuid/v1)

;;  => #uuid "005b7570-b3d4-11e4-a03e-3af93c3de9ae"

user> (uuid/v1)

;;  => #uuid "018a0a60-b3d4-11e4-a03e-3af93c3de9ae"

user> (uuid/v1)

;;  => #uuid "02621ae0-b3d4-11e4-a03e-3af93c3de9ae"

```


V1 identifiers are the fastest kind of UUID to generate -- about 25%
faster than calling the JVM's built-in static method for generating ids,
`#'java.util.UUID/randomUUID`.


```
user> (criterium.core/bench (uuid/v1))

Evaluation count : 41142600 in 60 samples of 685710 calls.
Execution time mean : 1.499075 µs
```

### Random (v4) Identifiers


V4 identifiers are generated by directly invoking the static method
`#'java.util.UUID/randomUUID` and are, in typical situations, slower
to generate in addition to being non-deterministically unique.


```
user> (criterium.core/bench (uuid/v4))

Evaluation count : 31754100 in 60 samples of 529235 calls.
Execution time mean : 1.928087 µs
```

#### Repeatable Construction



### Namespaced (v3/v5) Identifiers


### Hybrid (non-standard) Identifiers


## API


_[function]_    `v0 []`

> Return the null UUID, #uuid "00000000-0000-0000-0000-000000000000"


_[function]_    `v1 []`

>  Generate a v1 (time-based) unique identifier, guaranteed to be unique
>  and thread-safe regardless of clock precision or degree of concurrency.
>  Creation of v1 UUID's does not require any call to a cryptographic 
>  generator and can be accomplished much more efficiently than v1, v3, v5,
>  or squuid's.  A v1 UUID reveals both the identity of the computer that 
>  generated the UUID and the time at which it did so.  Its uniqueness across 
>  computers is guaranteed as long as MAC addresses are not duplicated.


_[function]_    `v4 []`
_[function]_    `v4 [^long msb, ^long lsb]`

>  Generate a v4 (random) UUID.  Uses default JVM implementation.  If two
>  arguments, lsb and msb (both long) are provided, then construct a valid,
>  properly formatted v4 UUID based on those values.  So, for example the
>  following UUID, created from all zero bits, is indeed distinct from the
>  null UUID:
>
>      (v4)
>       => #uuid "dcf0035f-ea29-4d1c-b52e-4ea499c6323e"
>
>      (v4 0 0)
>       => #uuid "00000000-0000-4000-8000-000000000000"
>
>      (null)
>       => #uuid "00000000-0000-0000-0000-000000000000"


## Motivation

To a large extent, the design of the algorithmic
implementation is inspired by the Common-Lisp library
[_UNICLY_](http://github.com/mon-key/unicly) which is a painstakingly
optimized, encyclopaedic implementation of RFC-4122 the author of
which, Stan Pearman, has devoted considerable effort to research, refine, and
improve.  To my knowledge there is no more performant  and
precise implementation of the RFC-4122 specification available
anywhere, in any language, on any platform.

That having been said, this library intends to present a slightly more
comfortable public interface that places a little more priority on
convenient DWIM semantics at the cost of somewhat less emphasis on
low level performance optimizations.  Since this library is built as
an extension to the standard java.util.UUID class whose implementation
largely dominates its performance characteristics anyway, this seems to
be a reasonable philosophy.

## License

Copyright © 2013 Dan Lentz

Distributed under the Eclipse Public License either version 1.0 




