package org.runestar.cs2.util

internal class ListSet<T : Any>(val delegate: MutableCollection<T>) : AbstractMutableSet<T>() {

    override val size get() = delegate.size

    override fun add(element: T): Boolean = if (element in delegate) false else delegate.add(element)

    override fun iterator() = delegate.iterator()
}