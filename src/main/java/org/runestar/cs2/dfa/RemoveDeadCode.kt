package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Instruction

internal object RemoveDeadCode : Phase {

    override fun transform(func: Func) {
        var insn: Instruction? = func.instructions.first
        while (insn != null) {
            if (insn is Instruction.Return) {
                insn = func.instructions.next(insn)
                while (insn != null && insn !is Instruction.Label) {
                    val next = func.instructions.next(insn)
                    func.instructions.remove(insn)
                    insn = next
                }
            } else {
                insn = func.instructions.next(insn)
            }
        }
    }
}