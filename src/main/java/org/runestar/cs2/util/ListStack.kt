package org.runestar.cs2.util

class ListStack<T : Any>(
        val delegate: MutableList<T>
) {

    val size: Int get() = delegate.size

    fun isEmpty(): Boolean = delegate.isEmpty()

    fun push(element: T) { delegate.add(element) }

    fun peek(): T = delegate[delegate.lastIndex]

    fun pop(): T = delegate.removeAt(delegate.lastIndex)

    override fun hashCode(): Int = delegate.hashCode()

    override fun equals(other: Any?): Boolean = other is ListStack<*> && delegate == other.delegate

    override fun toString(): String = delegate.toString()
}