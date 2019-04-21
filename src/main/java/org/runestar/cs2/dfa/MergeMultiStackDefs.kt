package org.runestar.cs2.dfa

import org.runestar.cs2.Type
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn
import java.util.Collections

internal object MergeMultiStackDefs : Phase {

    override fun transform(func: Func) {
        for (insn in func.insns) {
            val assignment = insn as? Insn.Assignment ?: continue
            val defs = assignment.definitions
            if (defs.size <= 1) continue
            ds@
            for (i in defs.indices) {
                val d = defs[i]
                var c = func.insns.next(assignment)!!
                while (c is Insn.Assignment) {
                    if (c.expr == d) {
                        val cd = c.definitions.single()
                        cd.type = Type.bottom(cd.type, d.type)
                        defs[i] = cd
                        func.insns.remove(c)
                        continue@ds
                    }
                    c = func.insns.next(c)!!
                }
            }
            if (defs.all { it is Expr.Variable.Stack }) {
                val next = func.insns.next(assignment) as? Insn.Exprd
                val nextExpr = next?.expr as? Expr.Operation
                if (nextExpr != null && replaceExprList(assignment, nextExpr)) {
                    func.insns.remove(assignment)
                } else {
                    assignment.definitions = ArrayList()
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