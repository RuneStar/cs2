package org.runestar.cs2.util

fun <N : Any> DirectedGraph<N>.preOrder(
        n: N = head,
        seen: MutableSet<N> = HashSet(),
        consumer: (N) -> Boolean
) {
    if (!seen.add(n)) return
    if (!consumer(n)) return
    for (s in immediateSuccessors(n)) {
        preOrder(s, seen, consumer)
    }
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

fun <N : Any> DirectedGraph<N>.isPredecessor(
        n: N,
        predecessor: N,
        seen: MutableSet<N> = HashSet()
): Boolean {
    for (p in immediatePredecessors(n)) {
        if (!seen.add(p)) continue
        if (p == predecessor) return true
        if (isPredecessor(p, predecessor, seen)) return true
    }
    return false
}

fun <N : Any> DirectedGraph<N>.isSuccessor(
        n: N,
        successor: N,
        seen: MutableSet<N> = HashSet()
): Boolean {
    for (s in immediateSuccessors(n)) {
        if (!seen.add(s)) continue
        if (s == successor) return true
        if (isSuccessor(s, successor, seen)) return true
    }
    return false
}