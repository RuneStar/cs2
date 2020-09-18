package org.runestar.cs2.ir

import org.runestar.cs2.bin.StackType
import org.runestar.cs2.bin.Value
import org.runestar.cs2.ir.Variable as Var

interface Element : Expression {

    val stackType: StackType

    override val stackTypes: List<StackType> get() = listOf(stackType)

    class Constant(val value: Value) : Element {

        override val stackType get() = value.stackType

        override fun toString() = value.toString()
    }

    interface Variable : Element {

        val variable: Var

        override val stackType get() = variable.stackType
    }

    class Access(
            override val variable: Var,
            val value: Value? = null,
    ) : Variable {

        init {
            if (value != null) require(variable.stackType == value.stackType)
        }

        override fun toString() = "$$variable"
    }

    class Pointer(override val variable: Var) : Variable {

        override fun toString() = variable.toString()
    }
}
