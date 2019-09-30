package org.runestar.cs2.dfa

import org.runestar.cs2.Opcodes
import org.runestar.cs2.StackType
import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.Typing
import org.runestar.cs2.ir.VarId
import org.runestar.cs2.ir.VarSource
import org.runestar.cs2.ir.list

object FindArrayArgs : Phase.Individual() {

    override fun transform(f: Function) {
        if (f.arguments.none { it.typing.stackType == StackType.INT }) return
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val e = insn.expression
            if (e !is Expression.Operation) continue
            val args = e.arguments.list<Element>()
            when (e.id) {
                Opcodes.DEFINE_ARRAY -> {
                    if ((args[0] as Element.Variable).varId.id == 0) return
                }
                Opcodes.PUSH_ARRAY_INT -> {
                    if ((args[0] as Element.Variable).varId.id == 0) {
                        val newArgs = f.arguments.toMutableList()
                        newArgs[0] = Element.Variable(VarId(VarSource.ARRAY, 0), args[0].typing)
                        f.arguments = newArgs
                        return
                    }
                }
            }
        }
    }
}