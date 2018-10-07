package org.runestar.cs2.cg

import org.runestar.cs2.cfa.Construct
import org.runestar.cs2.ir.Func
import java.lang.Appendable

interface Generator {

    fun write(appendable: Appendable, func: Func, root: Construct)
}