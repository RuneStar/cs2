package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.plus

internal object CombineSameLineOperations : Phase {

    override fun transform(func: Func) {
        val itr = func.instructions.iterator()
        for (start in itr) {
            if (!isStackAssign(start)) continue
            var end = start
            while (isStackAssign(end)) end = itr.next()
            if (start != end && func.instructions.next(start) != end) {
                val newInsn = Instruction.Assignment(Expression(), Expression())
                func.instructions.insertBefore(newInsn, start)
                var n = start
                while (n != end) {
                    func.instructions.remove(n)
                    n as Instruction.Assignment
                    newInsn.expression = n.expression + newInsn.expression
                    newInsn.definitions = n.definitions + newInsn.definitions
                    n = func.instructions.next(n)!!
                }
            }
        }
    }

    private fun isStackAssign(insn: Instruction): Boolean {
        if (insn !is Instruction.Assignment) return false
        return insn.expression is Element.Variable.Stack
    }
}