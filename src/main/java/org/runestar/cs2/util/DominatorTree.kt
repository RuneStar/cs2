package org.runestar.cs2.util

fun <N : Any> dominatorTree(graph: DirectedGraph<N>): DirectedGraph<N> {
    // A Simple, Fast Dominance Algorithm. Cooper, Harvey, Kennedy. 2001
    val postOrderList = ArrayList<N>()
    val postOrderMap = HashMap<N, Int>()
    graph.postOrder {
        postOrderMap[it] = postOrderList.size
        postOrderList.add(it)
    }

    val idoms = HashMap<N, N>()

    fun intersect(n1: N, n2: N): N {
        var f1 = n1
        var f2 = n2
        while (f1 != f2) {
            val f2v = postOrderMap.getValue(f2)
            while (postOrderMap.getValue(f1) < f2v) {
                f1 = idoms.getValue(f1)
            }
            val f1v = postOrderMap.getValue(f1)
            while (postOrderMap.getValue(f2) < f1v) {
                f2 = idoms.getValue(f2)
            }
        }
        return f1
    }

//    idoms[graph.head] = graph.head
    var changed: Boolean
    do {
        changed = false
        for (i in (postOrderList.lastIndex - 1) downTo 0) {
            val n = postOrderList[i]
            val predecessors = graph.immediatePredecessors(n)
            var newIdom = predecessors[0]
            for (pi in 1..predecessors.lastIndex) {
                val p = predecessors[pi]
                if (p in idoms) {
                    newIdom = intersect(p, newIdom)
                }
            }
            changed = idoms.put(n, newIdom) != newIdom
        }
    } while (changed)

    val dominatorTree = LinkedGraph<N>()
    for ((n, idom) in idoms) {
        dominatorTree.addSuccessor(idom, n)
    }
    dominatorTree.head = graph.head
    return dominatorTree
}

