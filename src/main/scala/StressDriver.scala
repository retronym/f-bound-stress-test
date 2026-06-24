// Scala driver that references the F-bounded Java types so Zinc must extract
// and hash their API across the Java -> Scala boundary. If Zinc's classfile/API
// parser chokes on the deep F-bounds, bridges, or nested generic signatures,
// it surfaces here.

object StressDriver:
  def main(args: Array[String]): Unit =
    // Deep single-param F-bound chain.
    val leaf: L8[Leaf] = new Leaf
    println(s"leaf depth        : ${leaf.depth()}")

    // Force Zinc to extract the L0 <-> Mirror signature cycle, then walk it.
    val mirror: Mirror[Leaf] = leaf.companion()
    println(s"cycle lap         : ${mirror.origin().companion().pivot().depth()}")

    // Mutually-constrained F-bounds.
    val left: Carrier[LeftNode, RightNode] = new LeftNode
    println(s"carrier mutual    : ${left.right().left().getClass.getSimpleName}")

    // Mutually recursive type params across two constructors (Yin <-> Yang).
    val yin: Yin[ConcYin, ConcYang] = new ConcYin
    println(s"yin/yang          : ${yin.yang().yin().getClass.getSimpleName}")
    // Mutual bound within one method signature: A <: Comparable[B], B <: Comparable[A].
    println(s"mutual compare    : ${Utilities.mutualCompare("a", "b")}")

    // Wildcard-in-F-bound + covariant bridge.
    val wi: WildStress[Integer] = new WildInt(1)
    println(s"wildint compare   : ${wi.compareTo(new WildInt(2))}")

    // Covariant bridge proliferation.
    val tip: Top = new Tip
    println(s"tip produce       : ${tip.produce().getClass.getSimpleName}")

    // Diamond default methods.
    val d: DiamondLeft = new DiamondBottom
    println(s"diamond name      : ${d.name()}")

    // Multi-bound generic static method + wildcard hell.
    println(s"utilities sort    : ${Utilities.sortedCopy(java.util.List.of(3, 1, 2))}")

    // Enum implementing an F-bounded interface.
    println(s"transformer       : ${Transformer.IDENTITY.apply("ok")}")
