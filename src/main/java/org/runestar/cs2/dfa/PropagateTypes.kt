package org.runestar.cs2.dfa

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn

internal object PropagateTypes : Phase {

    override fun transform(func: Func) {
        while (
                propagateVars(func) or
                propagateComparisons(func) or
                propagateReturns(func) or
                propagateAssignments(func)
        ) { }
    }

    private fun propagateAssignments(func: Func): Boolean {
        var changed = false
        for (insn in func.insns) {
            if (insn !is Insn.Assignment) continue
            val defs = insn.definitions
            if (defs.size != 1) continue
            val expr = insn.expr
            if (expr !is Expr.Var) continue
            val def = defs.single()
            val type = Type.bottom(expr.type, def.type)
            if (type != expr.type || type != def.type) changed = true
            expr.type = type
            def.type = type
        }
        return changed
    }

    private fun propagateVars(func: Func): Boolean {
        return changeVarTypes(func, scanVars(func))
    }

    private fun propagateComparisons(func: Func): Boolean {
        var changed = false
        for (insn in func.insns) {
            if (insn !is Insn.Branch) continue
            val expr = (insn.expr as Expr.Operation)
            changed = propagateComparisons(expr) || changed
        }
        return changed
    }

    private fun propagateComparisons(branch: Expr.Operation): Boolean {
        var changed = false
        when (branch.id) {
            Opcodes.SS_OR, Opcodes.SS_AND -> {
                changed = propagateComparisons(branch.arguments[0] as Expr.Operation) || changed
                changed = propagateComparisons(branch.arguments[1] as Expr.Operation) || changed
            }
            Opcodes.BRANCH_EQUALS, Opcodes.BRANCH_NOT -> {
                val arg0 = branch.arguments[0]
                val arg1 = branch.arguments[1]
                val type = Type.bottom(arg0.type, arg1.type)
                if (arg0.type != type) changed = true
                if (arg1.type != type) changed = true
                arg0.type = type
                arg1.type = type
            }
        }
        return changed
    }

    private fun scanVars(func: Func): Map<Expr.Var, Type> {
        val map = HashMap<Expr.Var, Type>()
        for (arg in func.args) {
            addType(arg, map)
        }
        for (insn in func.insns) {
            if (insn !is Insn.Exprd) continue
            val expr = insn.expr
            if (insn is Insn.Assignment) {
                for (d in insn.definitions) {
                    addType(d, map)
                }
            }
            scanVars(expr, map)
        }
        return map
    }

    private fun addType(v: Expr.Var, map: MutableMap<Expr.Var, Type>) {
        map.compute(v) { k, t -> if (t == null) k.type else Type.bottom(t, k.type) }
    }

    private fun scanVars(e: Expr, map: MutableMap<Expr.Var, Type>) {
        if (e is Expr.Operation) {
            for (arg in e.arguments) {
                scanVars(arg, map)
            }
        } else if (e is Expr.Var) {
            addType(e, map)
        }
    }

    private fun changeVarTypes(func: Func, map: Map<Expr.Var, Type>): Boolean {
        var changed = false
        for (arg in func.args) {
            val s = arg.type
            arg.type = map.getOrDefault(arg, arg.type)
            if (arg.type != s) {
                changed = true
            }
        }
        for (insn in func.insns) {
            if (insn !is Insn.Exprd) continue
            val expr = insn.expr
            if (insn is Insn.Assignment) {
                for (d in insn.definitions) {
                    val before = d.type
                    d.type = map.getOrDefault(d, d.type)
                    if (before != d.type) {
                        changed = true
                    }
                }
                if (expr is Expr.Cst) {
                    expr.type = insn.definitions.single().type
                }
            }
            changed = changeVarTypes(expr, map) || changed
        }
        return changed
    }

    private fun changeVarTypes(e: Expr, map: Map<Expr.Var, Type>): Boolean {
        var changed = false
        if (e is Expr.Operation) {
            for (arg in e.arguments) {
                changed = changeVarTypes(arg, map) || changed
            }
        } else if (e is Expr.Var) {
            val before = e.type
            e.type = map.getOrDefault(e, e.type)
            if (e.type != before) {
                changed = true
            }
        }
        return changed
    }

    private fun propagateReturns(func: Func): Boolean {
        if (func.returns.isEmpty()) return false
        val returnTypes = scanReturns(func)
        func.returns = returnTypes
        return setReturns(func, returnTypes)
    }

    private fun scanReturns(func: Func): MutableList<Type> {
        val list = ArrayList<Type>()
        for (insn in func.insns) {
            if (insn is Insn.Return) {
                val args = (insn.expr as Expr.Operation).arguments
                if (list.isEmpty()) {
                    args.flatMapTo(list) { it.types }
                } else {
                    val li = list.listIterator()
                    val ri = args.flatMap { it.types }.iterator()
                    while (li.hasNext()) {
                        li.set(Type.bottom(li.next(), ri.next()))
                    }
                }
            }
        }
        return list
    }

    private fun setReturns(func: Func, returnTypes: List<Type>): Boolean {
        var changed = false
        for (insn in func.insns) {
            if (insn is Insn.Return) {
                val args = (insn.expr as Expr.Operation).arguments
                val ri = returnTypes.iterator()
                val li = args.iterator()
                while (ri.hasNext()) {
                    val next = li.next()
                    val before = next.types
                    val w = ArrayList<Type>()
                    repeat(before.size) {
                        w.add(ri.next())
                    }
                    next.types = w
                    if (before != next.types) {
                        changed = true
                    }
                }
            }
        }
        return changed
    }
}