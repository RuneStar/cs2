package org.runestar.cs2.cfa

import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Instruction
import java.util.ArrayList

interface Construct {

    var next: Construct?

    class Seq(val instructions: MutableList<Instruction> = ArrayList()) : Construct {

        override var next: Construct? = null
    }

    class Branch(var condition: Expression.Operation, var body: Construct)

    class If(val branches: MutableList<Branch> = ArrayList()) : Construct {

        var elze: Construct? = null

        override var next: Construct? = null
    }

    class While(val condition: Expression.Operation): Construct {

        lateinit var body: Construct

        override var next: Construct? = null
    }

    class Switch(
            val expression: Expression,
            val cases: Map<Set<Int>, Construct>
    ) : Construct {

        var default: Construct? = null

        override var next: Construct? = null
    }
}