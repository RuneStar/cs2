package org.runestar.cs2.util

internal data class ListStack<T : Any>(
        val delegate: MutableList<T> = ArrayList()
) {

    val size: Int get() = delegate.size

    fun isEmpty(): Boolean = delegate.isEmpty()

    fun push(element: T) { delegate.add(element) }

    fun peek(): T = delegate[delegate.lastIndex]

    fun pop(): T = delegate.removeAt(delegate.lastIndex)

    fun popAll(): MutableList<T> = delegate.toMutableList().also { delegate.clear() }

    fun pop(count: Int): MutableList<T> {
        val l = MutableList(count) {
            pop()
        }
        l.reverse()
        return l
    }
}