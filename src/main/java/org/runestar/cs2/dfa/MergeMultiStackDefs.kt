package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn

internal object MergeMultiStackDefs : Phase {

    override fun transform(func: Func) {
        for (insn in func.insns) {
            val assignment = insn as? Insn.Assignment ?: continue
            val defs = assignment.definitions
            if (defs.size <= 1) continue
            ds@
            for (d in defs) {
                var c = func.insns.next(assignment)!!
                while (c is Insn.Assignment) {
                    if (c.expr == d) {
                        d.id = c.definitions.single().id
                        func.insns.remove(c)
                        continue@ds
                    }
                    c = func.insns.next(c)!!
                }
            }
        }
    }
}