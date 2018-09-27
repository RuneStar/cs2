package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn
import org.runestar.cs2.util.DirectedGraph
import org.runestar.cs2.util.LinkedGraph
import org.runestar.cs2.util.PartitionedChain

typealias BasicBlock = PartitionedChain.Block<Insn>

fun buildBlocks(func: Func): DirectedGraph<BasicBlock> {
    val blockHeads = ArrayList<Insn>()
    blockHeads.add(func.insns.first)
    for (insn in func.insns) {
        when (insn) {
            is Insn.Label -> if (blockHeads.last() != insn) blockHeads.add(insn)
            is Insn.Branch, is Insn.Switch -> blockHeads.add(func.insns.next(insn)!!)
        }
    }
    val blockSet = PartitionedChain(func.insns, blockHeads)
    val graph = LinkedGraph<BasicBlock>()
    blockSet.blocks.forEachIndexed { i, block ->
        val last = block.tail
        when (last) {
            is Insn.Goto -> graph.addSuccessor(block, blockSet.of(last.label))
            is Insn.Switch -> {
                graph.addSuccessor(block, blockSet.blocks[i + 1])
                last.map.values.forEach { label -> graph.addSuccessor(block, blockSet.of(label)) }
            }
            is Insn.Branch -> {
                graph.addSuccessor(block, blockSet.of(last.pass))
                graph.addSuccessor(block, blockSet.blocks[i + 1])
            }
            is Insn.Return -> {}
            is Insn.Assignment, is Insn.Label -> graph.addSuccessor(block, blockSet.blocks[i + 1])
            else -> error(last)
        }
    }
    graph.head = blockSet.blocks.first()
    return graph
}