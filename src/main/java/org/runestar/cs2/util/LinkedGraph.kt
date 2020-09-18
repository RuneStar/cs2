package org.runestar.cs2.util

class LinkedGraph<N : Any> : DirectedGraph<N> {

    private val map = HashMap<N, Node<N>>()

    override lateinit var head: N

    override fun immediatePredecessors(n: N): List<N> = get(n).predecessors

    override fun immediateSuccessors(n: N): List<N> = get(n).successors

    override fun iterator(): Iterator<N> = map.keys.iterator()

    private fun get(n: N): Node<N> = map.getOrPut(n) { Node() }

    fun addSuccessor(n: N, successor: N) {
        get(n).successors.add(successor)
        get(successor).predecessors.add(n)
    }

    override fun toString() = buildString {
        for (n in this@LinkedGraph) {
            append(n).append(", s=").append(immediateSuccessors(n)).append(", p=").append(immediatePredecessors(n)).appendLine()
        }
    }

    private class Node<N : Any>(
            val predecessors: MutableList<N> = ArrayList(),
            val successors: MutableList<N> = ArrayList()
    )
}