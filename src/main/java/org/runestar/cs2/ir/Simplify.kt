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

fun replaceTrivialAssignments(func: Func) {
    outer@
    while (true) {
        for (i in func.insns.indices) {
            val insn = func.insns[i]
            if (insn !is Insn.Assignment) continue
            val defs = insn.definitions
            if (defs.size != 1) continue
            val expr = insn.expr
            if (expr !is Expr.Const && expr !is Expr.Var) continue
            val def = defs.single()
            walk(func, i, HashSet()) { n ->
                if (n !is Insn.Exprd) return@walk true
                var last = false
                if (n is Insn.Assignment && n.definitions == listOf(def)) {
                    last = true
                }
                val nexpr = n.expr
                when (nexpr) {
                    is Expr.Var -> {
                        if (nexpr == def) {
                            n.expr = expr
                        }
                    }
                    is Expr.Operation -> {
                        nexpr.arguments.replaceAll {
                            if (it == def) expr else it
                        }
                    }
                }
                return@walk !last
            }
            func.insns.remove(insn)
            continue@outer
        }
        return
    }
}

private fun walk(func: Func, index: Int, seen: MutableSet<Int>, consumer: (Insn) -> Boolean) {
    var pc = index
    while (true) {
        if (pc in seen) return
        val insn = func.insns[pc]
        seen.add(pc)
        if (index != pc) {
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