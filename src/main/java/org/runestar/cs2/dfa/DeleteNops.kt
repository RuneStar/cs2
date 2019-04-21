package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn

internal object DeleteNops : Phase {

    override fun transform(func: Func) {
        val itr = func.insns.iterator()
        for (insn in itr) {
            if (insn !is Insn.Assignment) continue
            if (insn.definitions.isNotEmpty()) continue
            val e = insn.expr
            if (e !is Expr.Variable.Stack) continue
            itr.remove()
        }
    }
}