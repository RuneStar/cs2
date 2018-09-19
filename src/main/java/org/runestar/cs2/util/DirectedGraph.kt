package org.runestar.cs2.util

interface DirectedGraph<N : Any> : Iterable<N> {

    val head: N

    fun immediatePredecessors(n: N): List<N>

    fun immediateSuccessors(n: N): List<N>
}