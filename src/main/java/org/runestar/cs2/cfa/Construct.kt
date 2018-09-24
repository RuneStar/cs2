package org.runestar.cs2.cfa

import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Insn
import java.util.*

interface Construct {

    var next: Construct?

    class Seq(
            val insns: MutableList<Insn> = ArrayList()
    ) : Construct {

        override var next: Construct? = null
    }

    class Branch(var condition: Expr.Operation, var construct: Construct)

    class If(
            val branches: MutableList<Branch> = ArrayList()
    ) : Construct {

        var elze: Construct? = null

        override var next: Construct? = null
    }

    class While(
            val condition: Expr.Operation
    ): Construct {

        lateinit var inside: Construct

        override var next: Construct? = null
    }

    class Switch(
            val expr: Expr,
            val map: Map<SortedSet<Int>, Construct>
    ) : Construct {

        override var next: Construct? = null
    }
}