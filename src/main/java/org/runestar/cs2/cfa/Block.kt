package org.runestar.cs2.cfa

import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.util.DirectedGraph
import org.runestar.cs2.util.LinkedGraph
import org.runestar.cs2.util.PartitionedChain

typealias BasicBlock = PartitionedChain<Instruction>.Block

fun partitionBlocks(f: Function): PartitionedChain<Instruction> {
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

fun graphBlocks(blockList: PartitionedChain<Instruction>): DirectedGraph<BasicBlock> {
    val graph = LinkedGraph<BasicBlock>()
    for (block in blockList) {
        when (val tail = block.tail) {
            is Instruction.Goto -> graph.addSuccessor(block, blockList.block(tail.label))
            is Instruction.Switch -> {
                graph.addSuccessor(block, blockList.next(block))
                tail.cases.values.forEach { label -> graph.addSuccessor(block, blockList.block(label)) }
            }
            is Instruction.Branch -> {
                graph.addSuccessor(block, blockList.block(tail.pass))
                graph.addSuccessor(block, blockList.next(block))
            }
            is Instruction.Return -> {}
            is Instruction.Assignment, is Instruction.Label -> graph.addSuccessor(block, blockList.next(block))
        }
    }
    graph.head = blockList.head
    return graph
}