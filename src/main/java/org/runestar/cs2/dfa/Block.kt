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
    val itr = func.insns.iterator()
    var addNext = false
    while (itr.hasNext()) {
        val insn = itr.next()
        if (addNext) {
            blockHeads.add(insn)
            addNext = false
        }
        when (insn) {
            is Insn.Label -> blockHeads.add(insn)
            is Insn.Branch, is Insn.Switch -> addNext = true
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

fun <N : Any> dfsDown(
        graph: DirectedGraph<PartitionedChain.Block<N>>,
        block: PartitionedChain.Block<N>,
        from: N,
        consumer: (PartitionedChain.Block<N>, N) -> Boolean
) {
    if (from != block.tail) {
        dfsDown(graph, block, block.chain.next(from)!!, HashSet(), consumer)
    } else {
        for (s in graph.immediateSuccessors(block)) {
            dfsDown(graph, s, s.head, HashSet(), consumer)
        }
    }
}

fun <N : Any> dfsUp(
        graph: DirectedGraph<PartitionedChain.Block<N>>,
        block: PartitionedChain.Block<N>,
        from: N,
        consumer: (PartitionedChain.Block<N>, N) -> Boolean
) {
    if (from != block.head) {
        dfsUp(graph, block, block.chain.previous(from)!!, HashSet(), consumer)
    } else {
        for (s in graph.immediatePredecessors(block)) {
            dfsUp(graph, s, s.tail, HashSet(), consumer)
        }
    }
}

fun <N : Any> dfsDown(
        graph: DirectedGraph<PartitionedChain.Block<N>>,
        block: PartitionedChain.Block<N>,
        from: N,
        seen: MutableSet<N> = HashSet(),
        consumer: (PartitionedChain.Block<N>, N) -> Boolean
) {
    for (n in block.iterator(from, block.tail)) {
        if (!seen.add(n)) return
        if (consumer(block, n)) return
    }
    for (s in graph.immediateSuccessors(block)) {
        dfsDown(graph, s, s.head, seen, consumer)
    }
}

fun <N : Any> dfsUp(
        graph: DirectedGraph<PartitionedChain.Block<N>>,
        block: PartitionedChain.Block<N>,
        from: N,
        seen: MutableSet<N> = HashSet(),
        consumer: (PartitionedChain.Block<N>, N) -> Boolean
) {
    for (n in block.reverseIterator(from, block.head)) {
        if (!seen.add(n)) return
        if (consumer(block, n)) return
    }
    for (s in graph.immediatePredecessors(block)) {
        dfsUp(graph, s, s.tail, seen, consumer)
    }
}