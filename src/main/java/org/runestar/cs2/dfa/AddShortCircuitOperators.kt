package org.runestar.cs2.dfa

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.util.Chain

internal object AddShortCircuitOperators : Phase {

    override fun transform(func: Func) {
        while (ors(func) || ands(func)) {}
    }

    private fun ors(func: Func): Boolean {
        val itr = func.instructions.iterator()
        while (itr.hasNext()) {
            val if1 = itr.next() as? Instruction.Branch ?: continue
            if (!itr.hasNext()) continue
            val if2 = itr.next() as? Instruction.Branch ?: continue
            if (if1.pass == if2.pass) {
                itr.remove()
                if1.expression = Expression.Operation(listOf(Type.BOOLEAN), Opcodes.SS_OR, Expression(if1.expression, if2.expression))
                return true
            }
        }
        return false
    }

    private fun ands(func: Func): Boolean {
        val itr = func.instructions.iterator()
        while (itr.hasNext()) {
            val if1 = itr.next() as? Instruction.Branch ?: continue
            val if2 = func.instructions.next(if1.pass) as? Instruction.Branch ?: continue

            val else1 = func.instructions.next(if1) as? Instruction.Goto ?: continue
            val else2 = func.instructions.next(if2)

            when (else2) {
                is Instruction.Goto -> {
                    if (else1.label == else2.label && isUnused(func.instructions, if1.pass)) {
                        func.instructions.remove(if1.pass)
                        func.instructions.remove(else2)
                        func.instructions.remove(if2)

                        if1.pass = if2.pass
                        if1.expression = Expression.Operation(listOf(Type.BOOLEAN), Opcodes.SS_AND, Expression(if1.expression, if2.expression))
                        return true
                    }
                }
                is Instruction.Label -> {
                    if (else1.label == else2 && isUnused(func.instructions, if1.pass)) {
                        func.instructions.remove(if1.pass)
                        func.instructions.remove(else1)
                        func.instructions.remove(else2)
                        func.instructions.remove(if2)

                        if1.pass = if2.pass
                        if1.expression = Expression.Operation(listOf(Type.BOOLEAN), Opcodes.SS_AND, Expression(if1.expression, if2.expression))
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isUnused(insns: Chain<Instruction>, label: Instruction.Label): Boolean {
        for (insn in insns.iterator(label, insns.last)) {
            if (insn is Instruction.Goto && insn.label == label) return false
        }
        return true
    }
}