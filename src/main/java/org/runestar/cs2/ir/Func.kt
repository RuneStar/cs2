package org.runestar.cs2.ir

import org.runestar.cs2.Type
import org.runestar.cs2.util.Chain

class Func(
        val id: Int,
        val arguments: List<Element.Variable.Local>,
        var instructions: Chain<Instruction>,
        var returnTypes: MutableList<Type>
) {

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("arguments=").appendln(arguments)
        instructions.forEachIndexed { index, insn ->
            sb.appendln(insn)
        }
        sb.append("returnTypes=").append(returnTypes)
        return sb.toString()
    }
}