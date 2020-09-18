package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.FunctionSet
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.asList

object DeleteNops : Phase.Individual() {

    override fun transform(f: Function, fs: FunctionSet) {
        val itr = f.instructions.iterator()
        for (insn in itr) {
            if (insn !is Instruction.Assignment) continue
            if (insn.definitions.asList.isNotEmpty()) continue
            if (insn.expression.asList.all { it is Element }) {
                itr.remove()
            }
        }
    }
}