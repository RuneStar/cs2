package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Func

internal interface Phase {

    fun transform(func: Func)

    class Composite(private vararg val ps: Phase) : Phase {

        override fun transform(func: Func) = ps.forEach { it.transform(func) }
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