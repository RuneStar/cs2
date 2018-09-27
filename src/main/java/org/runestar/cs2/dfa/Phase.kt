package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Func

interface Phase {

    fun transform(func: Func)

    class Composite(private vararg val ps: Phase) : Phase {

        override fun transform(func: Func) = ps.forEach { it.transform(func) }
    }

    companion object {
        val DEFAULT = Composite(
                RemoveIdentityOperations,
                DeleteNops,
                MergeSingleStackDefs,
                MergeMultiStackDefs,
                AddShortCircuitOperators,
                PropagateVarTypes
        )
    }
}