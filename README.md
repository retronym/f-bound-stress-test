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

## License

Apache License 2.0 — see [LICENSE](LICENSE).
