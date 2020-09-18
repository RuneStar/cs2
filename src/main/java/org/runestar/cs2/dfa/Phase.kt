package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.FunctionSet

interface Phase {

    fun transform(fs: FunctionSet)

    abstract class Individual : Phase {

        final override fun transform(fs: FunctionSet) = fs.functions.values.forEach { transform(it, fs) }

        abstract fun transform(f: Function, fs: FunctionSet)
    }

    class Composite(private vararg val ps: Phase) : Phase {

        override fun transform(fs: FunctionSet) = ps.forEach { it.transform(fs) }
    }

    companion object {

        val DEFAULT = Composite(
                RemoveDeadCode,
                DeleteNops,
                ReorderArgs,
                FindArrayArgs,
                CombineSameLineOperations,
                InlineStackDefinitions,
                CalcTypes,
                CalcIdentifiers,
                AddShortCircuitOperators,
        )
    }
}