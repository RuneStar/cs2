package org.runestar.cs2.dfa

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn

object AddShortCircuitOperators : Phase {

    override fun transform(func: Func) {
        ors(func)
        ands(func)
    }

    private fun ors(func: Func) {
        outer@
        while (true) {
            val itr = func.insns.iterator()
            while (itr.hasNext()) {
                val if1 = itr.next() as? Insn.Branch ?: continue
                if (!itr.hasNext()) continue
                val if2 = itr.next() as? Insn.Branch ?: continue
                if (if1.pass == if2.pass) {
                    itr.remove()
                    if1.expr = Expr.Operation(listOf(Type.BOOLEAN), Opcodes.SS_OR, mutableListOf(if1.expr, if2.expr))
                    continue@outer
                }
            }
            return
        }
    }

    private fun ands(func: Func) {
        outer@
        while (true) {
            val itr = func.insns.iterator()
            while (itr.hasNext()) {
                val if1 = itr.next() as? Insn.Branch ?: continue
                val if2 = func.insns.next(if1.pass) as? Insn.Branch ?: continue

                val else1 = func.insns.next(if1) as? Insn.Goto ?: continue
                val else2 = func.insns.next(if2) as? Insn.Goto ?: continue

                if (else1.label == else2.label) {
                    func.insns.remove(if1.pass)
                    func.insns.remove(else2)
                    func.insns.remove(if2)

                    if1.pass = if2.pass
                    if1.expr = Expr.Operation(listOf(Type.BOOLEAN), Opcodes.SS_AND, mutableListOf(if1.expr, if2.expr))
                    continue@outer
                }
            }
            return
        }
    }
}