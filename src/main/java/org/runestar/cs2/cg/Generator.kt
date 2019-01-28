package org.runestar.cs2.cg

import org.runestar.cs2.cfa.Construct
import org.runestar.cs2.ir.Func

interface Generator {

    fun write(appendable: StringBuilder, func: Func, root: Construct)
}