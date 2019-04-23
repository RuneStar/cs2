package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction

internal object RemoveDeadCode : Phase {

    override fun transform(f: Function) {
        var insn: Instruction? = f.instructions.first
        while (insn != null) {
            if (insn is Instruction.Return) {
                insn = f.instructions.next(insn)
                while (insn != null && insn !is Instruction.Label) {
                    val next = f.instructions.next(insn)
                    f.instructions.remove(insn)
                    insn = next
                }
            } else {
                insn = f.instructions.next(insn)
            }
        }
    }
}