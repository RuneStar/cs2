package org.runestar.cs2.cfa

import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.util.isSuccessorAcyclic

fun reconstruct(f: Function): Construct {
    val fg = FlowGraph(f)
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
        if (insn is Instruction.Label) continue
        if (insn == block.tail) continue
        seq.instructions.add(insn)
    }
    when (val tail = block.tail) {
        is Instruction.Return -> {
            seq.instructions.add(tail)
        }
        is Instruction.Goto -> {
            return reconstructBlock(flow, seq, dominator, flow.blocks.block(tail.label))
        }
        is Instruction.Switch -> {
            var after: BasicBlock? = null
            val cases = LinkedHashMap<Set<Int>, Construct>()
            val switch = Construct.Switch(tail.expression, cases)
            seq.next = switch
            for (v in tail.cases.values.toSet()) {
                val keys = LinkedHashSet<Int>()
                for (e in tail.cases) {
                    if (e.value == v) keys.add(e.key)
                }
                val nxt = Construct.Seq()
                cases[keys] = nxt
                val dst = flow.blocks.block(v)
                after = reconstructBlock(flow, nxt, dst, dst) ?: after
            }
            val next = flow.blocks.next(block)
            val default = Construct.Seq()
            switch.default = default
            after = reconstructBlock(flow, default, next, next) ?: after
            if (default.instructions.isEmpty() && default.next == null) {
                switch.default = null
            }
            if (after != null) {
                return reconstructBlock(flow, switch, block, after)
            }
        }
        is Instruction.Branch -> {
            val pass = flow.blocks.block(tail.pass)
            val fail = flow.blocks.next(block)
            val preds = flow.graph.immediatePredecessors(block)
            if (preds.any { it.index > block.index }) {
                val w = Construct.While(tail.expression as Expression.Operation)
                seq.next = w
                w.body = Construct.Seq()
                reconstructBlock(flow, w.body, pass, pass)
                return reconstructBlock(flow, w, fail, fail)
            } else {
                val iff = Construct.If()
                seq.next = iff
                val branch = Construct.Branch(tail.expression as Expression.Operation, Construct.Seq())
                iff.branches.add(branch)
                val afterIf = reconstructBlock(flow, branch.body, pass, pass)
                val elze = Construct.Seq()
                iff.elze = elze
                val afterElze = reconstructBlock(flow, elze, fail, fail)
                if (elze.instructions.isEmpty() && elze.next == null) {
                    iff.elze = null
                } else if (elze.instructions.isEmpty() && elze.next is Construct.If && elze.next!!.next == null) {
                    val if2 = elze.next as Construct.If
                    iff.branches.addAll(if2.branches)
                    iff.elze = if2.elze
                }

                if (afterIf == null) {
                    iff.next = iff.elze
                    iff.elze = null
                    if (afterElze != null) {
                        return reconstructBlock(flow, iff.next ?: iff, block, afterElze)
                    }
                } else {
                    return reconstructBlock(flow, iff, block, afterIf)
                }
            }
        }
        is Instruction.Assignment -> {
            seq.instructions.add(tail)
            return reconstructBlock(flow, seq, dominator, flow.blocks.next(block))
        }
        is Instruction.Label -> {
            return reconstructBlock(flow, seq, dominator, flow.blocks.next(block))
        }
    }
    return null
}