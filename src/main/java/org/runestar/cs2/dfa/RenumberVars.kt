package org.runestar.cs2.dfa

import org.runestar.cs2.TopType
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn

object RenumberVars : Phase {

    override fun transform(func: Func) {
        val intMap = HashMap<Int, Int>()
        val strMap = HashMap<Int, Int>()

        fun renameVar(v: Expr.Var) {
            val id = when (v.type.topType) {
                TopType.STRING -> strMap.getOrPut(v.id) { strMap.size }
                TopType.INT -> intMap.getOrPut(v.id) { intMap.size }
            }
            v.id = id
        }

        for (a in func.args) {
            renameVar(a)
        }

        fun renameExpr(expr: Expr) {
            when (expr) {
                is Expr.Var -> renameVar(expr)
                is Expr.Operation -> {
                    for (e in expr.arguments) {
                        renameExpr(e)
                    }
                }
            }
        }

        fun renameInsn(insn: Insn) {
            if (insn is Insn.Assignment) {
                for (d in insn.definitions) {
                    renameVar(d)
                }
            }
            if (insn is Insn.Exprd) {
                renameExpr(insn.expr)
            }
        }

        for (insn in func.insns) {
            renameInsn(insn)
        }
    }
}