package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.FunctionSet

object ReorderArgs : Phase.Individual() {

    override fun transform(f: Function, fs: FunctionSet) {
        if (f.arguments.size <= 1) return
        val oldStackTypes = f.arguments.map { it.stackType }
        val typing = fs.typings.args(f.id, oldStackTypes)
        val newStackTypes = typing.map { it.stackType }
        if (oldStackTypes == newStackTypes) return
        val oldArgs = f.arguments.toMutableList()
        val newArgs = newStackTypes.map { st -> oldArgs.removeAt(oldArgs.indexOfFirst { it.stackType == st }) }
        f.arguments = newArgs
    }
}