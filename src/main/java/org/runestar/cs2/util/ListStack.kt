package org.runestar.cs2.util

internal data class ListStack<T : Any>(
        val delegate: MutableList<T> = ArrayList()
) {

    constructor(initialCapacity: Int) : this(ArrayList(initialCapacity))

    val size: Int get() = delegate.size

    fun isEmpty(): Boolean = delegate.isEmpty()

    fun push(element: T) { delegate.add(element) }

    fun peek(): T = delegate[delegate.lastIndex]

    fun pop(): T = delegate.removeAt(delegate.lastIndex)

    fun popAll(): List<T> = delegate.reversed().also { delegate.clear() }
}