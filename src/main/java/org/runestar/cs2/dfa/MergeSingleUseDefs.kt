package org.runestar.cs2.dfa

import org.runestar.cs2.Type
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn
import org.runestar.cs2.util.DirectedGraph

object MergeSingleUseDefs : Phase {

    override fun transform(func: Func) {
        val graph = buildBlocks(func)
        dfsDown(graph, graph.head, graph.head.head, HashSet()) { block, insn ->
            val def = insn.singleDef() ?: return@dfsDown false
            val uses = uses(graph, block, insn as Insn.Assignment)
            if (uses.isEmpty()) {
                if (insn.expr is Expr.Operation) {
                    insn.definitions = emptyList()
                } else {
                    block.remove(insn)
                }
            } else if (uses.size == 1) {
                val use = uses.first()
                if (singleDef(graph, use.first, use.second, def)) {
                    block.remove(insn)
                    replaceExpr(use.second, def, insn.expr)
                }
            }
            false
        }
    }

    private fun Insn.singleDef(): Expr.Var? {
        if (this !is Insn.Assignment) return null
        if (definitions.size != 1) return null
        return definitions.single()
    }

    private fun uses(
            graph: DirectedGraph<BasicBlock>,
            block: BasicBlock,
            insn: Insn.Assignment
    ): List<Pair<BasicBlock, Insn.Exprd>> {
        val def = insn.definitions.single()
        val uses: MutableList<Pair<BasicBlock, Insn.Exprd>> = ArrayList()
        dfsDown(graph, block, insn) { b, n ->
            val allExprs = subExprs(n)
            val count = allExprs.count { it == def }
            repeat(count) {
                uses.add(b to (n as Insn.Exprd))
            }
            return@dfsDown n.assigns(def)
        }
        return uses
    }

    private fun singleDef(
            graph: DirectedGraph<BasicBlock>,
            block: BasicBlock,
            insn: Insn.Exprd,
            use: Expr.Var
    ): Boolean {
        var count = 0
        dfsUp(graph, block, insn) { b, n ->
            val allDefs = definitions(n)
            val ncount = allDefs.count { it == use }
            count += ncount
            return@dfsUp ncount > 0
        }
        return count == 1
    }

    private fun subExprs(insn: Insn): List<Expr> {
        if (insn !is Insn.Exprd) return emptyList()
        return subExprs(insn.expr)
    }

    private fun Insn.assigns(expr: Expr.Var): Boolean {
        return this is Insn.Assignment && definitions.contains(expr)
    }

    private fun subExprs(expr: Expr): List<Expr> {
        return when (expr) {
            is Expr.Cst, is Expr.Var -> listOf(expr)
            is Expr.Operation -> expr.arguments.flatMap { subExprs(it) }
            else -> error(expr)
        }
    }

    private fun definitions(insn: Insn): List<Expr.Var> {
        if (insn !is Insn.Assignment) return emptyList()
        return insn.definitions
    }

    private fun replaceExpr(insn: Insn.Exprd, a: Expr, by: Expr) {
        val insnExpr = insn.expr
        if (insnExpr == a) {
            by.type = Type.bottom(insn.expr.type, by.type)
            insn.expr = by
            return
        }
        val op = insnExpr as? Expr.Operation ?: return
        replaceExpr(op, a, by)
    }

    private fun replaceExpr(op: Expr.Operation, a: Expr, by: Expr) {
        for (arg in op.arguments) {
            if (arg is Expr.Operation) {
                replaceExpr(arg, a, by)
            }
        }
        op.arguments.replaceAll {
            if (it == a) {
                by.type = Type.bottom(it.type, by.type)
                by
            } else {
                it
            }
        }
    }
}