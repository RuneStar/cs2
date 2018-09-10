package org.runestar.cs2.ir

import java.util.*

class SparseStack<T>(private val elements: TreeMap<Int, T>, size: Int, private val default: T) {

    constructor(default: T) : this(TreeMap(), 0, default)

    var size = size
        private set

    val lastIndex: Int get() = size - 1

    fun pop(): T = elements.remove(--size) ?: default

    fun popN(count: Int) {
        size -= count
        if (elements.isEmpty()) return
        if (size == 0 || elements.firstKey() >= size) return elements.clear()
        val keys = elements.navigableKeySet().descendingIterator()
        while (keys.hasNext() && keys.next() >= size) {
            keys.remove()
        }
    }

    fun peek(): T = get(lastIndex)

    fun push(element: T) { elements[size++] = element }

    fun push() { size++ }

    fun pushN(count: Int) { size += count }

    fun clear() {
        size = 0
        elements.clear()
    }

    fun copy() = SparseStack(TreeMap(elements), size, default)

    private fun get(index: Int): T = elements[index] ?: default

    override fun toString(): String {
        return (0 until size).joinToString(", ", "[", "]") { get(it).toString() }
    }
}