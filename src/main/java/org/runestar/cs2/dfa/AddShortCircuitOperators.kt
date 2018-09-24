package org.runestar.cs2.dfa

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn
import org.runestar.cs2.util.Chain

object AddShortCircuitOperators : Phase {

    override fun transform(func: Func) {
        while (ors(func) || ands(func)) {}
    }

    private fun ors(func: Func): Boolean {
        val itr = func.insns.iterator()
        while (itr.hasNext()) {
            val if1 = itr.next() as? Insn.Branch ?: continue
            if (!itr.hasNext()) continue
            val if2 = itr.next() as? Insn.Branch ?: continue
            if (if1.pass == if2.pass) {
                itr.remove()
                if1.expr = Expr.Operation(listOf(Type.BOOLEAN), Opcodes.SS_OR, mutableListOf(if1.expr, if2.expr))
                return true
            }
        }
        return false
    }

    private fun ands(func: Func): Boolean {
        val itr = func.insns.iterator()
        while (itr.hasNext()) {
            val if1 = itr.next() as? Insn.Branch ?: continue
            val if2 = func.insns.next(if1.pass) as? Insn.Branch ?: continue

            val else1 = func.insns.next(if1) as? Insn.Goto ?: continue
            val else2 = func.insns.next(if2) as? Insn.Goto ?: continue

            if (else1.label == else2.label && isUnused(func.insns, if1.pass)) {
                func.insns.remove(if1.pass)
                func.insns.remove(else2)
                func.insns.remove(if2)

                if1.pass = if2.pass
                if1.expr = Expr.Operation(listOf(Type.BOOLEAN), Opcodes.SS_AND, mutableListOf(if1.expr, if2.expr))
                return true
            }
        }
        return false
    }

    private fun isUnused(insns: Chain<Insn>, label: Insn.Label): Boolean {
        for (insn in insns.iterator(label, insns.last)) {
            if (insn is Insn.Goto && insn.label == label) return false
        }
        return true
    }
}