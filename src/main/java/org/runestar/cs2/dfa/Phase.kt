package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Function

interface Phase {

    fun transform(fs: Map<Int, Function>)

    abstract class Individual : Phase {

        final override fun transform(fs: Map<Int, Function>) = fs.values.forEach { transform(it) }

        abstract fun transform(f: Function)
    }

    class Composite(private vararg val ps: Phase) : Phase {

        override fun transform(fs: Map<Int, Function>) = ps.forEach { it.transform(fs) }
    }

    companion object {

        val DEFAULT = Composite(
                RemoveDeadCode,
                DeleteNops,
                FindArrayArgs,
                InferTypes,
                CombineSameLineOperations,
                InlineStackDefinitions,
                AddShortCircuitOperators
        )
    }
}