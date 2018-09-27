package org.runestar.cs2.dfa

import org.runestar.cs2.Type
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn

object PropagateVarTypes : Phase {

    override fun transform(func: Func) {
        changeTypes(func, scan(func))
    }

    private fun scan(func: Func): Map<Expr.Var, Type> {
        val map = HashMap<Expr.Var, Type>()
        for (insn in func.insns) {
            if (insn !is Insn.Exprd) continue
            val expr = insn.expr
            if (insn is Insn.Assignment) {
                for (d in insn.definitions) {
                    addType(d, map)
                }
            }
            scan(expr, map)
        }
        return map
    }

    private fun addType(v: Expr.Var, map: MutableMap<Expr.Var, Type>) {
        map.compute(v) { k, t -> if (t == null) k.type else Type.bottom(t, k.type) }
    }

    private fun scan(e: Expr, map: MutableMap<Expr.Var, Type>) {
        if (e is Expr.Operation) {
            for (arg in e.arguments) {
                scan(arg, map)
            }
        } else if (e is Expr.Var) {
            addType(e, map)
        }
    }

    private fun changeTypes(func: Func, map: Map<Expr.Var, Type>) {
        for (arg in func.args) {
            arg.type = map.getOrDefault(arg, arg.type)
        }
        for (insn in func.insns) {
            if (insn !is Insn.Exprd) continue
            val expr = insn.expr
            if (insn is Insn.Assignment) {
                for (d in insn.definitions) {
                    d.type = map.getOrDefault(d, d.type)
                }
                expr.types = insn.definitions.map { it.type }
            }
            changeTypes(expr, map)
        }
    }

    private fun changeTypes(e: Expr, map: Map<Expr.Var, Type>) {
        if (e is Expr.Operation) {
            for (arg in e.arguments) {
                changeTypes(arg, map)
            }
        } else if (e is Expr.Var) {
            e.type = map.getOrDefault(e, e.type)
        }
    }
}