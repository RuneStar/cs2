package org.runestar.cs2.ir

import org.runestar.cs2.Opcodes

fun removeIdentityOperations(func: Func) {
    func.insns.forEach { insn ->
        if (insn !is Insn.Exprd) return@forEach
        val expr = insn.expr as? Expr.Operation ?: return@forEach
        when (expr.id) {
            Opcodes.PUSH_CONSTANT_INT, Opcodes.PUSH_CONSTANT_STRING,
            Opcodes.PUSH_INT_LOCAL, Opcodes.PUSH_STRING_LOCAL,
            Opcodes.POP_INT_LOCAL, Opcodes.POP_STRING_LOCAL -> {
                insn.expr = expr.arguments.single()
            }
        }
    }
}

fun removeNopInstructions(func: Func) {
    func.insns.removeIf { insn ->
        if (insn !is Insn.Exprd) return@removeIf false
        val expr = insn.expr as? Expr.Operation ?: return@removeIf false
        when (expr.id) {
            Opcodes.POP_INT_DISCARD, Opcodes.POP_STRING_DISCARD -> true
            else -> false
        }
    }
}

private fun walk(func: Func, index: Int, seen: MutableSet<Int>, consumer: (Insn) -> Boolean) {
    var pc = index
    while (true) {
        if (pc in seen) return
        val insn = func.insns[pc]
        if (index != pc) {
            seen.add(pc)
            if (!consumer(insn)) return
        }
        when (insn) {
            is Insn.Assignment, is Insn.Label -> pc++
            is Insn.Goto -> pc = func.insns.indexOf(insn.label)
            is Insn.Branch -> {
                pc++
                walk(func, func.insns.indexOf(insn.pass), seen, consumer)
            }
            is Insn.Switch -> {
                pc++
                insn.map.values.forEach { walk(func, func.insns.indexOf(it), seen, consumer) }
            }
            is Insn.Return -> return
        }
    }
}

fun replaceSingleUseAssignments(func: Func) {
    outer@
    while (true) {
        for (i in func.insns.indices) {
            val insn = func.insns[i]
            if (insn !is Insn.Assignment) continue
            val def = insn.definitions.singleOrNull() ?: continue
            var use: Insn.Exprd? = null
            var usesCount = 0
            walk(func, func.insns.indexOf(insn), HashSet()) { n ->
                var last = false
                if (n is Insn.Assignment && n.definitions.contains(def)) {
                    last = true
                }
                if (n is Insn.Exprd) {
                    val subexprs = subExprs(n.expr)
                    for (expr in subexprs) {
                        if (expr == def) {
                            use = n
                            usesCount++
                        }
                    }
                }
                return@walk !last
            }
            if (usesCount <= 1) {
                func.insns.remove(insn)
                if (usesCount == 1) {
                    replaceExpr(use!!, def, insn.expr)
                }
                continue@outer
            }
        }
        return
    }
}

private fun subExprs(expr: Expr): List<Expr> {
    return when (expr) {
        is Expr.Const, is Expr.Var -> listOf(expr)
        is Expr.Operation -> expr.arguments.flatMap { subExprs(it) }
        else -> error(expr)
    }
}

private fun replaceExpr(insn: Insn.Exprd, a: Expr, b: Expr) {
    val insnExpr = insn.expr
    if (insnExpr == a) {
        insn.expr = b
        return
    }
    val op = insnExpr as? Expr.Operation ?: return
    replaceExpr(op, a, b)
}

private fun replaceExpr(op: Expr.Operation, a: Expr, b: Expr) {
    for (arg in op.arguments) {
        if (arg is Expr.Operation) {
            replaceExpr(arg, a, b)
        }
    }
    op.arguments.replaceAll {
        if (it == a) b else it
    }
}