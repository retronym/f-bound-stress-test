# f-bound-stress-test

A small Java corpus that deliberately stresses naive class-file / API parsers —
the kind found in incremental compilers and build tooling (Zinc, sbt's API
extractor, `javap`-style walkers). It packs a lot of pathological-but-legal
generic signatures into one file so a parser that mishandles any of them fails
loudly and early.

## Stress vectors

| # | Vector | What it exercises |
|---|--------|-------------------|
| 1 | Deep single-parameter F-bound chain (`L0`…`L8`) | Signature-attribute depth; naive recursive parsers can blow up quadratically |
| 2 | Mutually-constrained F-bounds (`Carrier<A, B>`) | Two type parameters referencing each other through their bounds |
| 2b | Mutually recursive type params across constructors (`Yin`/`Yang`) + single-signature mutual bound (`mutualCompare`) | A 2-cycle in the bound graph through generic types; a bare variable cycle is illegal, this isn't |
| 3 | Wildcard capture inside an F-bound | `<T extends Comparable<? super T>>` signature explosion |
| 4 | Covariant-override bridge proliferation | One synthetic bridge method per level of a covariant-return chain |
| 5 | Raw / generic mixed inheritance | Erased supertype + unchecked bridge logic |
| 6 | Deeply nested generic inner classes | Inner signature encodes all enclosing type parameters |
| 7 | Default-method diamond | Synthetic forwarders for diamond-inherited defaults |
| 8 | Multi-bound generic method + `throws` | `&`-separated intersection bounds; method descriptor + exception table |
| 9 | Enum implementing an F-bounded interface | Anonymous-class `InnerClasses` entries per constant |

## Layout

```
src/main/java/FBoundedStress.java   the stress corpus (compiles & runs standalone)
src/main/scala/StressDriver.scala   references the Java types so Zinc must extract
                                    and hash their API across the Java→Scala boundary
build.sbt, project/                 sbt scaffold (Scala 3, JavaThenScala order)
```

## Building

Standalone (no sbt):

```sh
javac -d out src/main/java/FBoundedStress.java
java  -cp out FBoundedStress
```

Through sbt / Zinc — this is the interesting path, since the Scala driver forces
Zinc to extract the API of every F-bounded Java type:

```sh
sbt compile   # exercises Zinc's classfile/API extractor
sbt run       # runs StressDriver
```

Verified against javac 26, Scala 3.7.0, and sbt 1.10.7 (Zinc).

## Inspecting the signatures with reflection

[`SigDump`](src/main/java/SigDump.java) decodes the `Signature` class-file
attribute via `java.lang.reflect.Type` — the same data a Zinc-style API
extractor walks — and prints it back as source-like text:

```sh
javac -d out src/main/java/FBoundedStress.java src/main/java/SigDump.java
java  -cp out SigDump
# or through sbt:  sbt "runMain SigDump"
```

The F-bounds and mutual recursion round-trip exactly as written:

```
== L0 ==
  type params : <T extends L0<T>>          # self-referential bound
  method      : Mirror<T> companion()      # ┐ signature cycle:
== Mirror ==                               # │ L0.companion -> Mirror -> origin -> L0 ...
  type params : <T extends L0<T>>          # │
  method      : L0<T> origin()             # ┘

== Carrier ==
  type params : <A extends Carrier<A, B>, B extends Carrier<B, A>>

== Yin ==
  type params : <Y extends Yin<Y, Z>, Z extends Yang<Z, Y>>   # ┐ 2-cycle across
== Yang ==                                                    # │ two constructors
  type params : <Z extends Yang<Z, Y>, Y extends Yin<Y, Z>>   # ┘

== Utilities.mutualCompare ==
  <A extends Comparable<B>, B extends Comparable<A>> int mutualCompare(A, B)

== Utilities.roundTrip ==
  <T extends L8<T> & Serializable & Comparable<T>> T roundTrip(T)   # intersection bound
```

The covariant-bridge proliferation is visible too — `SigDump` lists declared
methods and synthetic bridges separately:

```
== Leaf ==      method: Leaf self()      + bridge: L0 self()
== ConcYin ==   method: ConcYang yang()  + bridge: Yang yang()
                method: ConcYin self()   + bridge: Yin self()
== WildStress == method: int compareTo(WildStress<T>) + bridge: int compareTo(Object)
```

### The detail that matters

`SigDump.name()` prints a `TypeVariable` as **just its name and never recurses
into `getBounds()`**:

```java
if (t instanceof TypeVariable<?> tv) return tv.getName();   // don't recurse into bounds -> would loop
```

That one line is the whole point of the corpus. Core reflection is safe because
`getTypeParameters()` returns *lazy* `TypeVariable` objects, so the JDK never
expands the cycle for you. But the reflected `Type` graph **is** cyclic: eagerly
following `T extends L0<T>` into its bound, or chasing
`companion() -> Mirror -> origin() -> L0`, loops forever unless the consumer
memoizes visited variables/types. Any printer, hasher, or API extractor that
walks these signatures must carry a visited-set — exactly the invariant this
corpus is built to break.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
