package org.runestar.cs2.cfa

import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn
import org.runestar.cs2.util.DirectedGraph
import org.runestar.cs2.util.LinkedGraph
import org.runestar.cs2.util.PartitionedChain

typealias BasicBlock = PartitionedChain<Insn>.Block

fun partitionBlocks(func: Func): PartitionedChain<Insn> {
    val blockHeads = ArrayList<Insn>()
    blockHeads.add(func.insns.first)
    for (insn in func.insns) {
        when (insn) {
            is Insn.Label -> if (blockHeads.last() != insn) blockHeads.add(insn)
            is Insn.Branch, is Insn.Switch -> blockHeads.add(func.insns.next(insn)!!)
        }
    }
    return PartitionedChain(func.insns, blockHeads)
}

fun graphBlocks(blockList: PartitionedChain<Insn>): DirectedGraph<BasicBlock> {
    val graph = LinkedGraph<BasicBlock>()
    blockList.forEach { block ->
        val last = block.tail
        when (last) {
            is Insn.Goto -> graph.addSuccessor(block, blockList.block(last.label))
            is Insn.Switch -> {
                graph.addSuccessor(block, blockList.next(block))
                last.map.values.forEach { label -> graph.addSuccessor(block, blockList.block(label)) }
            }
            is Insn.Branch -> {
                graph.addSuccessor(block, blockList.block(last.pass))
                graph.addSuccessor(block, blockList.next(block))
            }
            is Insn.Return -> {}
            is Insn.Assignment, is Insn.Label -> graph.addSuccessor(block, blockList.next(block))
            else -> error(last)
        }
    }
    graph.head = blockList.head
    return graph
}