package org.runestar.cs2.util

data class ListStack<T : Any>(val delegate: MutableList<T>) {

    val size: Int get() = delegate.size

    fun isEmpty(): Boolean = delegate.isEmpty()

    fun push(element: T) { delegate.add(element) }

    fun peek(): T = delegate[delegate.lastIndex]

    fun pop(): T = delegate.removeAt(delegate.lastIndex)

    fun popAll(): List<T> = delegate.toList().also { delegate.clear() }

    fun pop(count: Int): List<T> = MutableList(count) { pop() }.also { it.reverse() }
}