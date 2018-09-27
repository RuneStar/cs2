package org.runestar.cs2.cfa

import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn
import org.runestar.cs2.util.isSuccessorAcyclic
import java.util.*
import kotlin.collections.LinkedHashMap

internal fun reconstruct(func: Func): Construct {
    val fg = FlowGraph(func)
    val root = Construct.Seq()
    reconstructBlock(fg, root, fg.graph.head, fg.graph.head)
    return root
}

private fun reconstructBlock(
        flow: FlowGraph,
        prev: Construct,
        dominator: BasicBlock,
        block: BasicBlock
): BasicBlock? {
    if (dominator != block && !flow.dtree.isSuccessorAcyclic(dominator, block)) return block
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
            val suc = flow.blocks.block(tail.label)
            return reconstructBlock(flow, seq, dominator, suc)
        }
        is Insn.Switch -> {
            val map = LinkedHashMap<SortedSet<Int>, Construct>()
            val switch = Construct.Switch(tail.expr, map)
            seq.next = switch
            for (v in tail.map.values.toSet()) {
                val keys = TreeSet<Int>()
                for (e in tail.map) {
                    if (e.value == v) keys.add(e.key)
                }
                val nxt = Construct.Seq()
                map[keys] = nxt
                val dst = flow.blocks.block(v)
                reconstructBlock(flow, nxt, dst, dst)
            }
            val next = flow.blocks.next(block)
            val elze = Construct.Seq()
            switch.elze = elze
            val afterElze = reconstructBlock(flow, elze, next, next)
            if (elze.insns.isEmpty() && elze.next == null) {
                switch.elze = null
            }
            if (afterElze != null) {
                reconstructBlock(flow, switch, block, afterElze)
            }
        }
        is Insn.Branch -> {
            val pass = flow.blocks.block(tail.pass)
            val fail = flow.blocks.next(block)
            val preds = flow.graph.immediatePredecessors(block)
            if (preds.any { it.index > block.index }) {
                val whil = Construct.While(tail.expr as Expr.Operation)
                seq.next = whil
                whil.inside = Construct.Seq()
                reconstructBlock(flow, whil.inside, pass, pass)
                return reconstructBlock(flow, whil, fail, fail)
            } else {
                val iff = Construct.If()
                seq.next = iff
                val branch = Construct.Branch(tail.expr as Expr.Operation, Construct.Seq())
                iff.branches.add(branch)
                val afterIf = reconstructBlock(flow, branch.construct, pass, pass)
                val elze = Construct.Seq()
                iff.elze = elze
                val afterElze = reconstructBlock(flow, elze, fail, fail)
                if (elze.insns.isEmpty() && elze.next == null) {
                    iff.elze = null
                } else if (elze.insns.isEmpty() && elze.next is Construct.If && elze.next!!.next == null) {
                    val if2 = elze.next as Construct.If
                    iff.branches.addAll(if2.branches)
                    iff.elze = if2.elze
                }

                if (afterIf != null) {
                    return reconstructBlock(flow, iff, block, afterIf)
                } else if (afterElze != null) {
                    return reconstructBlock(flow, iff, block, afterElze)
                }
            }
        }
        is Insn.Assignment -> {
            seq.insns.add(tail)
            val suc = flow.blocks.next(block)
            return reconstructBlock(flow, seq, dominator, suc)
        }
        is Insn.Label -> {
            val suc = flow.blocks.next(block)
            return reconstructBlock(flow, seq, dominator, suc)
        }
    }
    return null
}