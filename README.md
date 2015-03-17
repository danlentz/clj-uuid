clj-uuid
========

> _"The intent of the UUID is to enable distributed systems to uniquely_ 
> _identify information without significant central coordination."_
> -- [_Wikipedia/UUID_](http://en.wikipedia.org/wiki/Universally_unique_identifier)

* * * * * *

[![Build Status](https://travis-ci.org/danlentz/clj-uuid.svg?branch=master)]
(https://travis-ci.org/danlentz/clj-uuid)[![Dependency Status](https://www.versioneye.com/clojure/danlentz:clj-uuid/0.1.4/badge.svg)](https://www.versioneye.com/clojure/danlentz:clj-uuid/0.1.4)


**clj-uuid** is a Clojure library for generation and utilization of
UUIDs (Universally Unique Identifiers) as described by
[**IETF RFC4122**](http://www.ietf.org/rfc/rfc4122.txt).

This library extends the standard Java UUID class to provide true v1
(time based) and v3/v5 (namespace based) identifier generation.
Additionally, a number of useful supporting utilities are provided to
support serialization and manipulation of these UUIDs in a simple,
efficient manner.

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



### The Most Recent Release

With Leiningen:

[![Clojars Project](http://clojars.org/danlentz/clj-uuid/latest-version.svg)](http://clojars.org/danlentz/clj-uuid)

### How is it better?

The JVM version only provides an automatic generator for random (v4)
and (non-namespaced) pseudo-v3 UUID's.  Where appropriate, this library
does use the internal JVM UUID implementation.  The benefit with this library
is that clj-uuid provides an easy way to get v1 and true namespaced v3 and
v5 UUIDs.  v3/v5 UUID's are necessary because many of the interesting things
that you can do with UUID's require namespaced identifiers. v1 UUIDs are
really useful because they can be generated about **10x faster** than v4's since
they don't need to call a cryptographic random number generator.


### How Big?

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


### Usage

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


#### Literal Syntax

UUID's have a convenient literal syntax supported by the clojure
reader.  The tag `#uuid` denotes that the following string literal
will be read as a UUID.  UUID's evaluate to themselves:

```clojure

user> #uuid "e6ff478d-9492-48dd-886d-23ec4c6385ee"

;;  => #uuid "e6ff478d-9492-48dd-886d-23ec4c6385ee"
```


#### The NULL (v0) Identifier

The special UUID, `#uuid "00000000-0000-0000-0000-000000000000"` is
known as the _null UUID_ or _version 0 UUID_ and can be useful for
representing special values such as _nil_ or _null-context_. One may
reference the null UUID declaratively or functionally, although it is
best to pick one convention and remain consistant.


```clojure

user> (uuid/null)

;;  => #uuid "00000000-0000-0000-0000-000000000000"


user> (uuid/v0)

;;  => #uuid "00000000-0000-0000-0000-000000000000"


user> uuid/+null+

;;  => #uuid "00000000-0000-0000-0000-000000000000"

```


#### Time Based (v1) Identifiers

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


V1 identifiers are the fastest kind of UUID to generate -- about _TEN TIMES
as fast as calling the JVM's built-in static method for generating UUIDs_,
`#'java.util.UUID/randomUUID`.


```
user> (criterium.core/bench (uuid/v1))

Evaluation count: 139356300 in 60 samples of 2322605 calls.
Execution time mean: 201.153073 ns


user> (criterium/bench (java.util.UUID/randomUUID))

Evaluation count : 30850980 in 60 samples of 514183 calls.
Execution time mean : 2.012861 µs

```


#### Random (v4) Identifiers


V4 identifiers are generated by directly invoking the static method
`#'java.util.UUID/randomUUID` and are, in typical situations, slower
to generate in addition to being non-deterministically unique. It
exists primarily because it is very simple to implement and because
randomly generated UUID's are essentially unguessable.  They can be
useful in that sense, for example to seed a UUID namespace as we will see
in a later example.


#### Namespaced (v3/v5) Identifiers

First of all, the only difference between v3 and v5 UUID's is that v3's
are computed using an MD5 digest algorithm and v5's are computed using SHA1.
It is generally considered that SHA1 is a superior hash, but MD5 is
computationally less expensive and so v3 may be preferred in
situations requiring slightly faster performance. As such, when we give
examples of namespaced identifiers, we will typically just use `v5` with
the understanding that `v3` could be used identically in each instance.

##### Namespaces

If you are familiar with Clojure _vars_, you already understand the
idea of _namespaced_ identifiers.  To resolve the value of a var, one
needs to know not only the _name_ of a var, but also the _namespace_
it resides in.  It is intuitively clear that vars `#'user/x` and
`#'library/x` are distinct.  Namespaced UUID's follow a similar
concept, however namespaces are themselves represented as UUID's.
Names are strings that encode a representation of a symbol or value in
the namespace of that identifier.  Given a namespace and a local-name,
one can always (re)construct the unique identifier that represents
it.  We can demonstrate a few examples constructed using several of
the canonical top level namespace UUIDs:

```clojure

user> (uuid/v5 uuid/+namespace-url+ "http://example.com/")

;;  => #uuid "0a300ee9-f9e4-5697-a51a-efc7fafaba67"

user> (uuid/v5 uuid/+namespace-x500+ "http://example.com/")

;;  => #uuid "0cb29677-4eaf-578f-ab9b-f9ac67c33cb9"


user> (uuid/v3 uuid/+namespace-dns+ "www.clojure.org")

;;  => #uuid "3bdca4f7-fc85-3a8b-9038-7626457527b0"


user> (uuid/v5 uuid/+namespace-oid+ "0.1.22.13.8.236.1")

;;  => #uuid "9989a7d2-b7fc-5b6a-84d6-556b0531a065"
```

You can see in each case that the local "name" string is given in some
well-definted format specific to each namespace.  This is a very
common convention, but not enforced.  It is perfectly valid to
construct a namespaced UUID from any literal string.

```clojure

user> (uuid/v5 uuid/+namespace-url+ "I am clearly not a URL")

;;  => #uuid "a167a791-e550-57ae-b20f-666ee47ce7c1"
```

As a matter of fact, the requirements for a valid the local-part
constituent are even more general than even just Strings.  Any kind of
object will do:

```clojure

user> (uuid/v5 uuid/+namespace-url+ :keyword)

;;  => #uuid "bc480d53-fba7-5e5f-8f33-6ad77880a007"

user> (uuid/v5 uuid/+namespace-url+ :keyword)

;;  => #uuid "bc480d53-fba7-5e5f-8f33-6ad77880a007"

user> (uuid/v5 uuid/+namespace-oid+ :keyword)

;;  => #uuid "9b3d8a3d-fadf-55b5-811f-12a0c50c3e86"



user> (uuid/v5 uuid/+null+ 'this-symbol)

;;  => #uuid "8b2941d5-e40b-5364-afcf-0008833715a2"

user> (uuid/v5 uuid/+null+ 'this-symbol)

;;  => #uuid "8b2941d5-e40b-5364-afcf-0008833715a2"


```

This will be most efficient for classes of object that have been
extended with the `UUIDNameBytes` protocol.  If one intends to do such
a thing fequently, it is a simple matter to specialize an
`as-byte-array` method which can extract a byte serialization that
represents the 'name' of an object, typically unique within some given
namespace.  Here is a simple example where one adds specialized support
for URLs to be quickly digested as the bytes of their string representation.


```clojure

(extend-protocol UUIDNameBytes java.net.URL
  (as-byte-array [this]
    (.getBytes (.toString this) StandardCharsets/UTF_8)))


(uuid/v5 uuid/+namespace-url+ "http://example.com/")

;; => #uuid "0a300ee9-f9e4-5697-a51a-efc7fafaba67"


(uuid/v5 uuid/+namespace-url+ (java.net.URL. "http://example.com/"))

;; => #uuid "0a300ee9-f9e4-5697-a51a-efc7fafaba67"

```


##### Hierarchical Namespace

Because each UUID denotes its own namespace, it is easy to compose v5
identifiers in order to represent hierarchical sub-namespaces.  This,
for example, can be used to assign unique identifiers based not only
on the content of a string but the unique identity representing its
source or provenance:

```clojure

user> (uuid/v5
        (uuid/v5 uuid/+namespace-url+ "http://example.com/")
        "resource1#")

;;  => #uuid "6a3944a4-f00e-5921-b8b6-2cea5a745132"


user> (uuid/v5
        (uuid/v5 uuid/+namespace-url+ "http://example.com/")
        "resource2#")

;;  => #uuid "98879e2a-8511-59ab-877d-ac6f8667866d"


user> (uuid/v5
        (uuid/v5 uuid/+namespace-url+ "http://other.com/")
        "resource1#")

;;  => #uuid "bc956d0c-7af3-5b81-89f2-a96e8f9fd1a8"


user> (uuid/v5
        (uuid/v5 uuid/+namespace-url+ "http://other.com/")
        "resource2#")

;;  => #uuid "a38b24fe-7ab8-58c8-a3f8-d3adb308260b"


```

Because UUID's and namespaces can be chained together like this, one
can be certain that the UUID resulting from a chain of calls such as
the following will be unique -- if and only if the original namespace
matches:


```clojure

user> (-> (uuid/v1)
        (uuid/v5 "one")
        (uuid/v5 "two")
        (uuid/v5 "three"))

;;  => #uuid "eb7a0c2b-eb0e-5bb2-9819-3c9edc2814fa"


user> (-> (uuid/v1)
        (uuid/v5 "one")
        (uuid/v5 "two")
        (uuid/v5 "three"))

;;  => #uuid "45e8c272-1660-57ba-8892-6844e1d3196a"

```


At each step, the local part string must be identical, in order for the the
final UUID to match:

```clojure

user> (-> uuid/+namespace-dns+
        (uuid/v5 "one")
        (uuid/v5 "two")
        (uuid/v5 "three"))

;;  => #uuid "617756cc-3b02-5a86-ad4a-ab3e1403dbd6"


user> (-> uuid/+namespace-dns+
        (uuid/v5 "two")
        (uuid/v5 "one")
        (uuid/v5 "three"))

;;  => #uuid "52d5453e-2aa1-53c1-b093-0ea20ef57ad1"

```

This capability can be used to represent uniqueness of a sequence of
computations in, for example, a transaction system such as the one
used in the graph-object database system
[de.setf.resource](http://github.com/lisp/de.setf.resource/) or this
interesting new [CQRS/ES Server](http://yuppiechef.github.io/cqrs-server/). 

### A Simple Example

Ok, so now you know how to use this nifty new UUID library and you are
burning up to do something awesome with UUID's... But, ah, hmmm... First
you need to figure out what exactly you want to do with them.  Well,
before you start working on your distributed cloud-based secret
weapon, here is a simple way you can generate cryptographically
secure activation keys for your draconian licensing scheme.

First, we pick a secret key.  We might pick a time-based id, or we might
begin with some secret namespace, secret identifier pair to compute that
initial namespace deterministically.  This is convenient, but not necessary
-- the time-based or random private key could also be stored in some form
of persistent memory.  As unguessability important to deter hackers, we will
choose a random namespace and record it in some secret, persistent storage
to ensure we can regenerate a user's activation code identically on-demand
in the future.

```clojure

user> (def +secret-weapon-licensing-namespace+ (uuid/v4))


user> (uuid/v5 +secret-weapon-licensing-namespace+ "joe@example.com")

;;  => #uuid "b6433d1e-d369-5282-8dbc-bdd3845c376c"


user> (uuid/v5 +secret-weapon-licensing-namespace+ "mom@knitting-arts.edu")

;;  => #uuid "81e4708c-85bb-5f3c-be56-bba4d8b0ac91"

```

Now, as the orders start rolling in for your product, you can crank out
secret weapon activation codes just as well as if you were Microsoft.
Each one will be keyed to a user's email address and is guaranteed
to be irreversible.  You will infuriate them with unreasonably high
maintence support contract fees and intractible licensing terms.
You truly are diabolical.


### Basic API

* * * * * *

#### Namespaces

_(var)_         `+null+`

> `#uuid "00000000-0000-0000-0000-000000000000"`


_(var)_         `+namespace-dns+`

> `#uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8"`


_(var)_         `+namespace-url+`

> `#uuid "6ba7b811-9dad-11d1-80b4-00c04fd430c8"`


_(var)_         `+namespace-oid+`

> `#uuid "6ba7b812-9dad-11d1-80b4-00c04fd430c8"`


_(var)_         `+namespace-x500+`

> `#uuid "6ba7b814-9dad-11d1-80b4-00c04fd430c8"`

* * * * * *

#### Generators

_(function)_    `v0 []`

> Return the null UUID, #uuid "00000000-0000-0000-0000-000000000000"


_(function)_    `v1 []`

>  Generate a v1 (time-based) unique identifier, guaranteed to be unique
>  and thread-safe regardless of clock precision or degree of concurrency.
>  Creation of v1 UUID's does not require any call to a cryptographic 
>  generator and can be accomplished much more efficiently than v1, v3, v5,
>  or squuid's.  A v1 UUID reveals both the identity of the computer that 
>  generated the UUID and the time at which it did so.  Its uniqueness across 
>  computers is guaranteed as long as MAC addresses are not duplicated.


_(function)_    `v3 [^UUID namespace ^Object local-name]`

>  Generate a v3 (name based, MD5 hash) UUID.


_(function)_    `v4 []`

_(function)_    `v4 [^long msb, ^long lsb]`

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


_(function)_    `v5 [^UUID namespace ^Object local-name]`

>  Generate a v5 (name based, SHA1 hash) UUID.


_(function)_    `squuid []`

>  Generate a SQUUID (sequential, random) unique identifier.  SQUUID's
>  are a nonstandard variation on v4 (random) UUIDs that have the
>  desirable property that they increase sequentially over time as well
>  as encode retrievably the posix time at which they were generated.
>  Splits and reassembles a v4 UUID to merge current POSIX
>  time (seconds since 12:00am January 1, 1970 UTC) with the most
>  significant 32 bits of the UUID
  

_(function)_    `monotonic-time []`

>  Return a monotonic timestamp (guaranteed always increasing)  based on 
>  the number of 100-nanosecond intervals elapsed since the adoption of the 
>  Gregorian calendar in the West, 12:00am Friday October 15, 1582 UTC.

* * * * * *

#### Protocols

_(protocol)_    `UUIDNameBytes`

>  A mechanism intended for user-level extension that defines the
>  decoding rules for the local-part representation of arbitrary
>  Clojure / Java Objects when used for computing namespaced
>  identifiers.
>
> _(member)_    `as-byte-array [self]`
>
>>  Extract a byte serialization that represents the 'name' of x, 
>>  typically unique within a given namespace.


_(protocol)_    `UUIDable`

>  A UUIDable object directly represents a UUID.  Examples of things which
>  might be conceptually 'uuidable' include string representation of a
>  UUID in canonical hex format, or an appropriate URN URI.
>
> _(member)_    `as-uuid [self]`
>
>>  Coerce the value of `self` to a java.util.UUID.
>
> _(member)_    `uuidable? [self]`
>
>>  Return 'true' if `self` can be coerced to UUID.


_(protocol)_    `UUIDRfc4122`

>  A protocol that abstracts an Leach-Salz UUID as described by
>  IETF RFC4122 <http://www.ietf.org/rfc/rfc4122.txt>. A UUID
>  represents a 128-bit value, however there are variant encoding
>  layouts used to assign and interpret information encoded in
>  those bits.  This is a protocol for  _variant 2_ (*Leach-Salz*) 
>  UUID's.
>
> _(member)_    `hash-code [self]`
>
>>  Return a suitable 64-bit hash value for `self`.  Extend for
>>  specialized hash computation.
>
> _(member)_    `null? [self]`
>
>>  Return `true` only if `self` has all 128 bits set ot zero and is 
>>  therefore equal to the null UUID, 00000000-0000-0000-0000-000000000000.
>
> _(member)_    `uuid? [self]`
>
>> Return `true` if the class of `self` implements an RFC4122 unique identifier.




### References

* _A Universally Unique IDentifier (UUID) URN Namespace_  [IETF RFC-4122](http://www.ietf.org/rfc/rfc4122.txt)

* Wikipedia: [_Universally unique identifier_](http://en.wikipedia.org/wiki/Universally_unique_identifier)

* Reference Implementation: [CL-UUID](http://www.dardoria.net/software/uuid.html)

* Reference Implementation: [UNICLY](https://github.com/mon-key/unicly)

* [java.util.UUID](http://docs.oracle.com/javase/6/docs/api/java/util/UUID.html)

* [The web of names, hashes and UUIDs](http://joearms.github.io/2015/03/12/The_web_of_names.html)

### License

Copyright © 2015 Dan Lentz

Distributed under the Eclipse Public License version 1.0 


