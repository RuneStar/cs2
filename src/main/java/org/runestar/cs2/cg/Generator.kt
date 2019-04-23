package org.runestar.cs2.cg

import org.runestar.cs2.cfa.Construct
import org.runestar.cs2.ir.Function

interface Generator {

    fun write(buf: StringBuilder, f: Function, root: Construct)
}