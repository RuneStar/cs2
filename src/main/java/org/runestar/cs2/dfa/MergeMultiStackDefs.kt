package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn
import java.util.*

internal object MergeMultiStackDefs : Phase {

    override fun transform(func: Func) {
        for (insn in func.insns) {
            val assignment = insn as? Insn.Assignment ?: continue
            val defs = assignment.definitions
            if (defs.size <= 1) continue
            ds@
            for (d in defs) {
                var c = func.insns.next(assignment)!!
                while (c is Insn.Assignment) {
                    if (c.expr == d) {
                        d.id = c.definitions.single().id
                        func.insns.remove(c)
                        continue@ds
                    }
                    c = func.insns.next(c)!!
                }
            }
            if (defs.all { it.id < 0 }) {
                val next = func.insns.next(assignment) as? Insn.Exprd
                val nextExpr = next?.expr as? Expr.Operation
                if (nextExpr != null && replaceExprList(assignment, nextExpr)) {
                    func.insns.remove(assignment)
                } else {
                    assignment.definitions = emptyList()
                }
            }
        }
    }

    private fun replaceExprList(assignment: Insn.Assignment, expr: Expr.Operation): Boolean {
        val idx = Collections.indexOfSubList(expr.arguments, assignment.definitions)
        if (idx != -1) {
            val len = assignment.definitions.size
            expr.arguments[idx] = assignment.expr
            repeat(len - 1) {
                expr.arguments.removeAt(idx + 1)
            }
            return true
        } else {
            for (arg in expr.arguments) {
                if (arg is Expr.Operation) {
                    if (replaceExprList(assignment, arg)) return true
                }
            }
            return false
        }
    }
}