package org.runestar.cs2.util

interface DirectedGraph<N : Any> : Iterable<N> {

    val head: N

    fun immediatePredecessors(n: N): List<N>

    fun immediateSuccessors(n: N): List<N>
}

fun <N : Any> DirectedGraph<N>.postOrder(
        n: N = head,
        seen: MutableSet<N> = HashSet(),
        consumer: (N) -> Unit
) {
    if (!seen.add(n)) return
    for (s in immediateSuccessors(n)) {
        postOrder(s, seen, consumer)
    }
    consumer(n)
}

fun <N : Any> DirectedGraph<N>.isSuccessorAcyclic(
        n: N,
        successor: N
): Boolean {
    for (s in immediateSuccessors(n)) {
        if (s == successor) return true
        if (isSuccessorAcyclic(s, successor)) return true
    }
    return false
}