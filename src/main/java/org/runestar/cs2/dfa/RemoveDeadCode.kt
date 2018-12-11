package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn

object RemoveDeadCode : Phase {

    override fun transform(func: Func) {
        var insn: Insn? = func.insns.first
        while (insn != null) {
            if (insn is Insn.Return) {
                insn = func.insns.next(insn)
                while (insn != null && insn !is Insn.Label) {
                    val next = func.insns.next(insn)
                    func.insns.remove(insn)
                    insn = next
                }
            } else {
                insn = func.insns.next(insn)
            }
        }
    }
}