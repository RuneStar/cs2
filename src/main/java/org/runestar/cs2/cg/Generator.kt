package org.runestar.cs2.cg

import org.runestar.cs2.cfa.Construct
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.FunctionSet

interface Generator {

    fun write(f: Function, fs: FunctionSet, root: Construct)
}