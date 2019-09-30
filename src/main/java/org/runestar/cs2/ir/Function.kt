package org.runestar.cs2.ir

import org.runestar.cs2.util.Chain

class Function(
        val id: Int,
        var arguments: List<Element.Variable>,
        var instructions: Chain<Instruction>,
        var returnTypes: List<Typing>
) {

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("id=").appendln(id)
        sb.append("arguments=").appendln(arguments)
        instructions.forEach { sb.appendln(it) }
        sb.append("returnTypes=").append(returnTypes)
        return sb.toString()
    }
}