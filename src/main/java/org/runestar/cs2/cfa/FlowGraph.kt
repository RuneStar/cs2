package org.runestar.cs2.cfa

import org.runestar.cs2.ir.Func
import org.runestar.cs2.util.dominatorTree

class FlowGraph(val func: Func) {

    val blocks = partitionBlocks(func)

    val graph = graphBlocks(blocks)

    val dtree = dominatorTree(graph)
}