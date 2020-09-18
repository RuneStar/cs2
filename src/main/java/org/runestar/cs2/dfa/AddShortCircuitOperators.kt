package org.runestar.cs2.dfa

import org.runestar.cs2.bin.*
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.FunctionSet
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.util.Chain

object AddShortCircuitOperators : Phase.Individual() {

    override fun transform(f: Function, fs: FunctionSet) {
        while (ors(f) || ands(f));
    }

    private fun ors(f: Function): Boolean {
        val itr = f.instructions.iterator()
        while (itr.hasNext()) {
            val if1 = itr.next() as? Instruction.Branch ?: continue
            val if2 = f.instructions.next(if1) as? Instruction.Branch ?: continue
            if (if1.pass == if2.pass) {
                itr.remove()
                if2.expression = Expression.Operation(emptyList(), SS_OR, Expression(if1.expression, if2.expression))
                return true
            }
        }
        return false
    }

    private fun ands(f: Function): Boolean {
        val itr = f.instructions.iterator()
        while (itr.hasNext()) {
            val if1 = itr.next() as? Instruction.Branch ?: continue
            val if2 = f.instructions.next(if1.pass) as? Instruction.Branch ?: continue

            val else1 = f.instructions.next(if1) as? Instruction.Goto ?: continue
            val else2 = f.instructions.next(if2)

            when (else2) {
                is Instruction.Goto -> {
                    if (else1.label == else2.label && isUnused(f.instructions, if1.pass)) {
                        f.instructions.remove(if1.pass)
                        f.instructions.remove(else2)
                        f.instructions.remove(if2)

                        if1.pass = if2.pass
                        if1.expression = Expression.Operation(emptyList(), SS_AND, Expression(if1.expression, if2.expression))
                        return true
                    }
                }
                is Instruction.Label -> {
                    if (else1.label == else2 && isUnused(f.instructions, if1.pass)) {
                        f.instructions.remove(if1.pass)
                        f.instructions.remove(else1)
                        f.instructions.remove(else2)
                        f.instructions.remove(if2)

                        if1.pass = if2.pass
                        if1.expression = Expression.Operation(emptyList(), SS_AND, Expression(if1.expression, if2.expression))
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