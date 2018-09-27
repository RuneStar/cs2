package org.runestar.cs2.util

interface Chain<E : Any> : MutableIterable<E> {

    val first: E

    val last: E

    fun addFirst(e: E)

    fun addLast(e: E)

    fun next(e: E): E?

    fun previous(e: E): E?

    fun insertAfter(e: E, point: E)

    fun insertBefore(e: E, point: E)

    fun remove(e: E)

    override fun iterator(): MutableIterator<E> = iterator(first, last)

    fun iterator(from: E, to: E): MutableIterator<E>
}