package org.runestar.cs2.dfa

import org.runestar.cs2.Opcodes
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn

internal object RemoveIdentityOperations : Phase {

    override fun transform(func: Func) {
        func.insns.forEach { insn ->
            if (insn !is Insn.Exprd) return@forEach
            val expr = insn.expr as? Expr.Operation ?: return@forEach
            when (expr.id) {
                Opcodes.PUSH_CONSTANT_INT, Opcodes.PUSH_CONSTANT_STRING,
                Opcodes.PUSH_INT_LOCAL, Opcodes.PUSH_STRING_LOCAL,
                Opcodes.POP_INT_LOCAL, Opcodes.POP_STRING_LOCAL -> {
                    insn.expr = expr.arguments.single()
                }
            }
        }
    }
}