package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.VarSource
import org.runestar.cs2.ir.list

internal object DeleteNops : Phase.Individual() {

    override fun transform(f: Function) {
        val itr = f.instructions.iterator()
        for (insn in itr) {
            if (insn !is Instruction.Assignment) continue
            val defs = insn.definitions.list<Element.Variable>()
            if (defs.isNotEmpty()) continue
            val e = insn.expression.list<Expression>().singleOrNull() ?: continue
            if (e !is Element.Variable || e.source != VarSource.STACK) continue
            itr.remove()
        }
    }
}