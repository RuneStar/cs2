package org.runestar.cs2.dfa

import org.runestar.cs2.Opcodes
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn

internal object DeleteNops : Phase {

    override fun transform(func: Func) {
        val itr = func.insns.iterator()
        while (itr.hasNext()) {
            val insn = itr.next()
            if (insn !is Insn.Exprd) continue
            val expr = insn.expr as? Expr.Operation ?: continue
            when (expr.id) {
                Opcodes.POP_INT_DISCARD, Opcodes.POP_STRING_DISCARD -> itr.remove()
            }
        }
    }
}