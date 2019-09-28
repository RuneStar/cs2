package org.runestar.cs2.dfa

import org.runestar.cs2.ArrayType
import org.runestar.cs2.Opcodes
import org.runestar.cs2.Primitive
import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.VarSource
import org.runestar.cs2.ir.list

object FindArrayArgs : Phase.Individual() {

    override fun transform(f: Function) {
        if (f.arguments.firstOrNull()?.type != Primitive.INT) return
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val e = insn.expression
            if (e !is Expression.Operation) continue
            val args = e.arguments.list<Element>()
            when (e.id) {
                Opcodes.DEFINE_ARRAY -> {
                    if ((args[0] as Element.Variable).id == 0) return
                }
                Opcodes.PUSH_ARRAY_INT -> {
                    if ((args[0] as Element.Variable).id == 0) {
                        val newArgs = f.arguments.toMutableList()
                        newArgs[0] = Element.Variable(VarSource.ARRAY, 0, ArrayType.INT)
                        f.arguments = newArgs
                        return
                    }
                }
            }
        }
    }
}