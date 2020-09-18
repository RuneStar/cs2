package org.runestar.cs2.ir

import org.runestar.cs2.bin.StackType
import org.runestar.cs2.util.Chain

class Function(
        val id: Int,
        var arguments: List<Variable.Local>,
        val instructions: Chain<Instruction>,
        val returnTypes: List<StackType>
) {

    override fun toString() = buildString {
        append("id=").appendLine(id)
        append("arguments=").appendLine(arguments)
        append("returnTypes=").appendLine(returnTypes)
        instructions.forEach { appendLine(it) }
    }
}