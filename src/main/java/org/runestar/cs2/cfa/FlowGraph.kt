package org.runestar.cs2.cfa

import org.runestar.cs2.ir.Function
import org.runestar.cs2.util.dominatorTree

class FlowGraph(val f: Function) {

    val blocks = partitionBlocks(f)

    val graph = graphBlocks(blocks)

    val dtree = dominatorTree(graph)
}