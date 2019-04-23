package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.list
import java.util.Collections

internal object InlineStackDefinitions : Phase {

    override fun transform(f: Function) {
        out@
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            if (insn.definitions.list<Element.Variable>().any { it !is Element.Variable.Stack }) continue
            val defs = insn.definitions.list<Element.Variable.Stack>()
            if (defs.isEmpty()) continue
            var a: Instruction? = f.instructions.next(insn)!!
            while (a is Instruction.Evaluation) {
                if (replaceExprs(a, defs, insn.expression)) {
                    f.instructions.remove(insn)
                    continue@out
                }
                a = f.instructions.next(a)
            }
            if (insn.expression is Element.Constant) {
                f.instructions.remove(insn)
            } else {
                insn.definitions = Expression()
            }
        }
    }

    private fun replaceExprs(dst: Instruction.Evaluation, defs: List<Element.Variable.Stack>, by: Expression): Boolean {
        val e = dst.expression
        if (e is Expression.Operation) {
            val newArgs = replaceSubExpr(e.arguments.list(), defs, by) ?: return false
            e.arguments = Expression(newArgs)
            return true
        }
        val newE = replaceSubExpr(e.list(), defs, by) ?: return false
        dst.expression = Expression(newE)
        return true
    }

    private fun replaceSubExpr(es: List<Expression>, defs: List<Element.Variable.Stack>, by: Expression): List<Expression>? {
        val idx = Collections.indexOfSubList(es, defs)
        if (idx == -1) return null
        val newEs = ArrayList<Expression>(es.subList(0, idx))
        newEs.add(by)
        newEs.addAll(es.subList(idx + defs.size, es.size))
        return newEs
    }
}