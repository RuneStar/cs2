package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.FunctionSet
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.Variable
import org.runestar.cs2.ir.remove

object RemoveDeadCode : Phase.Individual() {

    override fun transform(f: Function, fs: FunctionSet) {
        var insn: Instruction? = f.instructions.first
        while (insn != null) {
            if (insn is Instruction.Return) {
                insn = f.instructions.next(insn)
                while (insn != null && insn !is Instruction.Label) {
                    if (insn is Instruction.Assignment) {
                        val def = insn.definitions as Element.Access
                        val v = def.variable as Variable.Stack
                        fs.typings.remove(v)
                        val c = insn.expression as Element.Constant
                        fs.typings.remove(c)
                    }
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