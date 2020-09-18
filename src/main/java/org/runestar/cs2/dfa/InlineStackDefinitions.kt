package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.FunctionSet
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.Typings
import org.runestar.cs2.ir.Variable
import org.runestar.cs2.ir.asList
import org.runestar.cs2.ir.replace
import java.util.Collections

object InlineStackDefinitions : Phase.Individual() {

    override fun transform(f: Function, fs: FunctionSet) {
        out@
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val defs = insn.definitions.asList as List<Element.Access>
            if (defs.any { it.variable !is Variable.Stack }) continue
            if (defs.isEmpty()) continue
            val expr = insn.expression
            var a: Instruction? = f.instructions.next(insn)!!
            while (a is Instruction.Evaluation) {
                if (replaceExprs(a, defs, expr, fs.typings)) {
                    f.instructions.remove(insn)
                    continue@out
                }
                a = f.instructions.next(a)
            }
            if (expr is Element.Constant) {
                fs.typings.remove(defs.single().variable)
                fs.typings.remove(expr)
                f.instructions.remove(insn)
            } else {
                for (d in defs) {
                    fs.typings.remove(d.variable)
                }
                insn.definitions = Expression()
            }
        }
    }

    private fun replaceExprs(dst: Instruction.Evaluation, defs: List<Element.Access>, by: Expression, typings: Typings): Boolean {
        val e = dst.expression
        replaceSubExpr(e.asList, defs, by, typings)?.let {
            dst.expression = it
            return true
        }
        when (e) {
            is Expression.Operation -> {
                replaceSubExpr(e.arguments.asList, defs, by, typings)?.let {
                    e.arguments = it
                    return true
                }
            }
            is Expression.Proc -> {
                replaceSubExpr(e.arguments.asList, defs, by, typings)?.let {
                    e.arguments = it
                    return true
                }
            }
            is Expression.ClientScript -> {
                val component = e.component
                if (defs.size == 1 && component is Element.Access) {
                    if (defs.single().variable == component.variable) {
                        replace(typings.of(component), typings.of(by).single())
                        typings.variables.remove(component.variable)
                        e.component = by
                        return true
                    }
                }
                replaceSubExpr(e.arguments.asList, defs, by, typings)?.let {
                    e.arguments = it
                    return true
                }
                replaceSubExpr(e.triggers.asList, defs, by, typings)?.let {
                    e.triggers = it
                    return true
                }
            }
        }
        return false
    }


    private fun replaceSubExpr(es1: List<Expression>, es2: List<Element.Access>, by: Expression, typings: Typings): Expression? {
        val st1 = es1.map { if (it is Element.Access) it.variable else null }
        val st2 = es2.map { it.variable }
        val idx = Collections.indexOfSubList(st1, st2)
        if (idx == -1) return null
        val es3 = ArrayList<Expression>(es1.size - es2.size + 1)
        es3.addAll(es1.subList(0, idx))
        es3.add(by)
        es3.addAll(es1.subList(idx + es2.size, es1.size))
        replace(typings.of(Expression(es2)), typings.of(by))
        es2.forEach { typings.variables.remove(it.variable) }
        return Expression(es3)
    }
}