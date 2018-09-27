package org.runestar.cs2.util

internal fun <N : Any> DirectedGraph<N>.postOrder(
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

internal fun <N : Any> DirectedGraph<N>.isSuccessorAcyclic(
        n: N,
        successor: N
): Boolean {
    for (s in immediateSuccessors(n)) {
        if (s == successor) return true
        if (isSuccessorAcyclic(s, successor)) return true
    }
    return false
}