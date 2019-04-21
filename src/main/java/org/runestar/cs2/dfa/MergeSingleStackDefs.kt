package org.runestar.cs2.dfa

import org.runestar.cs2.Type
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn

internal object MergeSingleStackDefs : Phase {

    override fun transform(func: Func) {
        outer@
        for (insn in func.insns) {
            val assignment = insn as? Insn.Assignment ?: continue
            val def = assignment.definitions.singleOrNull() ?: continue
            if (def !is Expr.Variable.Stack) continue
            var c: Insn? = func.insns.next(assignment)!!
            while (c != null && c is Insn.Exprd) {
                if (replace(c, def, assignment.expr)) {
                    func.insns.remove(assignment)
                    continue@outer
                }
                c = func.insns.next(c)
            }
            if (assignment.expr is Expr.Cst) {
                func.insns.remove(assignment)
            } else if (assignment.expr is Expr.Operation) {
                assignment.definitions = ArrayList()
            }
        }
    }

    private fun replace(insn: Insn.Exprd, v: Expr.Variable.Stack, by: Expr): Boolean {
        if (insn.expr == v) {
            by.type = Type.bottom(insn.expr.type, by.type)
            insn.expr = by
            if (insn is Insn.Assignment) {
                insn.definitions.single().type = by.type
            }
            return true
        }
        val opExpr = insn.expr
        if (opExpr is Expr.Operation) {
            return replace(opExpr, v, by)
        }
        return false
    }

    private fun replace(operation: Expr.Operation, v: Expr.Variable.Stack, by: Expr): Boolean {
        val li = operation.arguments.listIterator()
        while (li.hasNext()) {
            val next = li.next()
            if (next is Expr.Operation && replace(next, v, by)) {
                return true
            }
            if (next == v) {
                by.type = Type.bottom(next.type, by.type)
                li.set(by)
                return true
            }
        }
        return false
    }
}