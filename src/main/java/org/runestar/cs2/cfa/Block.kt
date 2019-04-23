package org.runestar.cs2.cfa

import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.util.DirectedGraph
import org.runestar.cs2.util.LinkedGraph
import org.runestar.cs2.util.PartitionedChain

internal typealias BasicBlock = PartitionedChain<Instruction>.Block

internal fun partitionBlocks(f: Function): PartitionedChain<Instruction> {
    val blockHeads = ArrayList<Instruction>()
    blockHeads.add(f.instructions.first)
    for (insn in f.instructions) {
        when (insn) {
            is Instruction.Label -> if (blockHeads.last() != insn) blockHeads.add(insn)
            is Instruction.Branch, is Instruction.Switch -> blockHeads.add(f.instructions.next(insn)!!)
        }
    }
    return PartitionedChain(f.instructions, blockHeads)
}

internal fun graphBlocks(blockList: PartitionedChain<Instruction>): DirectedGraph<BasicBlock> {
    val graph = LinkedGraph<BasicBlock>()
    blockList.forEach { block ->
        val last = block.tail
        when (last) {
            is Instruction.Goto -> graph.addSuccessor(block, blockList.block(last.label))
            is Instruction.Switch -> {
                graph.addSuccessor(block, blockList.next(block))
                last.map.values.forEach { label -> graph.addSuccessor(block, blockList.block(label)) }
            }
            is Instruction.Branch -> {
                graph.addSuccessor(block, blockList.block(last.pass))
                graph.addSuccessor(block, blockList.next(block))
            }
            is Instruction.Return -> {}
            is Instruction.Assignment, is Instruction.Label -> graph.addSuccessor(block, blockList.next(block))
            else -> error(last)
        }
    }
    graph.head = blockList.head
    return graph
}