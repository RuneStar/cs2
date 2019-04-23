package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Function

internal interface Phase {

    fun transform(f: Function)

    class Composite(private vararg val ps: Phase) : Phase {

        override fun transform(f: Function) = ps.forEach { it.transform(f) }
    }

    companion object {
        val DEFAULT = Composite(
                RemoveDeadCode,
                DeleteNops,
                PropagateTypes,
                CombineSameLineOperations,
                InlineStackDefinitions,
                AddShortCircuitOperators
        )
    }
}