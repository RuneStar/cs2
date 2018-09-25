package org.runestar.cs2.cfa

import org.runestar.cs2.dfa.BasicBlock
import org.runestar.cs2.dfa.buildBlocks
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn
import org.runestar.cs2.util.DirectedGraph
import org.runestar.cs2.util.dominatorTree
import org.runestar.cs2.util.isSuccessorAcyclic
import java.util.*
import kotlin.collections.LinkedHashMap

fun reconstruct(func: Func): Construct {
    val graph = buildBlocks(func)
    val dtree = dominatorTree(graph)
    val root = Construct.Seq()
    reconstructBlock(graph, dtree, root, graph.head, graph.head)
    return root
}

private fun reconstructBlock(
        graph: DirectedGraph<BasicBlock>,
        dtree: DirectedGraph<BasicBlock>,
        prev: Construct,
        dominator: BasicBlock,
        block: BasicBlock
): BasicBlock? {
    if (dominator != block && !dtree.isSuccessorAcyclic(dominator, block)) return block
    val seq = if (prev is Construct.Seq) {
        prev
    } else {
        val s = Construct.Seq()
        prev.next = s
        s
    }
    for (insn in block) {
        if (insn is Insn.Label) continue
        if (insn == block.tail) continue
        seq.insns.add(insn)
    }
    val tail = block.tail
    when (tail) {
        is Insn.Return -> {
            seq.insns.add(tail)
        }
        is Insn.Goto -> {
            val suc = graph.immediateSuccessors(block).single()
            return reconstructBlock(graph, dtree, seq, dominator, suc)
        }
        is Insn.Switch -> {
            val map = LinkedHashMap<SortedSet<Int>, Construct>()
            val switch = Construct.Switch(tail.expr, map)
            seq.next = switch
            val successors = graph.immediateSuccessors(block)
            for (v in tail.map.values.toSet()) {
                val keys = tail.map.filter { it.value == v }.mapTo(TreeSet()) { it.key }
                val nxt = Construct.Seq()
                map[keys] = nxt
                val dst = successors.first { it.head == v }
                reconstructBlock(graph, dtree, nxt, dst, dst)
            }
            val next = successors.first { it.index == block.index + 1 }
            val elze = Construct.Seq()
            switch.elze = elze
            val afterElze = reconstructBlock(graph, dtree, elze, next, next)
            if (elze.insns.isEmpty() && elze.next == null) {
                switch.elze = null
            }
            if (afterElze != null) {
                reconstructBlock(graph, dtree, switch, block, afterElze)
            }
        }
        is Insn.Branch -> {
            val successors = graph.immediateSuccessors(block)
            val pass = successors.first { it.head == tail.pass }
            val fail = successors.first { it.index == block.index + 1 }
            val preds = graph.immediatePredecessors(block)
            if (preds.any { it.index > block.index }) {
                val whil = Construct.While(tail.expr as Expr.Operation)
                seq.next = whil
                whil.inside = Construct.Seq()
                reconstructBlock(graph, dtree, whil.inside, pass, pass)
                return reconstructBlock(graph, dtree, whil, fail, fail)
            } else {
                val iff = Construct.If()
                seq.next = iff
                val branch = Construct.Branch(tail.expr as Expr.Operation, Construct.Seq())
                iff.branches.add(branch)
                val afterIf = reconstructBlock(graph, dtree, branch.construct, pass, pass)
                val elze = Construct.Seq()
                iff.elze = elze
                val afterElze = reconstructBlock(graph, dtree, elze, fail, fail)
                if (elze.insns.isEmpty() && elze.next == null) {
                    iff.elze = null
                } else if (elze.insns.isEmpty() && elze.next is Construct.If && elze.next!!.next == null) {
                    val if2 = elze.next as Construct.If
                    iff.branches.addAll(if2.branches)
                    iff.elze = if2.elze
                }

                if (afterIf != null) {
                    return reconstructBlock(graph, dtree, iff, block, afterIf)
                } else if (afterElze != null) {
                    return reconstructBlock(graph, dtree, iff, block, afterElze)
                }
            }
        }
        is Insn.Assignment -> {
            seq.insns.add(tail)
            val suc = graph.immediateSuccessors(block).single()
            return reconstructBlock(graph, dtree, seq, dominator, suc)
        }
        is Insn.Label -> {
            val suc = graph.immediateSuccessors(block).single()
            return reconstructBlock(graph, dtree, seq, dominator, suc)
        }
    }
    return null
}