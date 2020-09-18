package org.runestar.cs2.util

class HashChain<E : Any> : Chain<E> {

    private val map = HashMap<E, Link>()

    private var head: E? = null

    private var tail: E? = null

    override val first: E get() = checkNotNull(head)

    override val last: E get() = checkNotNull(tail)

    private inner class Link(val e: E) {

        var next: Link? = null

        var previous: Link? = null

        fun unlink() = bind(previous, next)

        fun bind(a: Link?, b: Link?) {
            if (a == null) {
                head = b?.e
            } else {
                a.next = b
            }
            if (b == null) {
                tail = a?.e
            } else {
                b.previous = a
            }
        }

        fun insertBefore(e: E): Link {
            val newLink = Link(e)
            bind(previous, newLink)
            bind(newLink, this)
            return newLink
        }

        fun insertAfter(e: E): Link {
            val newLink = Link(e)
            bind(newLink, next)
            bind(this, newLink)
            return newLink
        }
    }

    override fun insertAfter(e: E, point: E) {
        map[e] = map.getValue(point).insertAfter(e)
    }

    override fun insertBefore(e: E, point: E) {
        map[e] = map.getValue(point).insertBefore(e)
    }

    override fun addFirst(e: E) {
        if (head != null) {
           insertBefore(e, first)
        } else {
            map[e] = Link(e)
            head = e
            tail = e
        }
    }

    override fun addLast(e: E) {
        if (tail != null) {
            insertAfter(e, last)
        } else {
            map[e] = Link(e)
            head = e
            tail = e
        }
    }

    override fun next(e: E): E? = map.getValue(e).next?.e

    override fun previous(e: E): E? = map.getValue(e).previous?.e

    override fun remove(e: E) = map.getValue(e).unlink()

    override fun iterator(from: E, to: E): MutableIterator<E> = LinkIterator(from, to)

    private inner class LinkIterator(
            from: E?,
            val to: E?
    ) : MutableIterator<E> {

        var curr: Link? = from?.let { map.getValue(it) }

        override fun hasNext(): Boolean = curr != null

        override fun next(): E {
            val last = curr!!
            curr = if (last.e == to) null else last.next
            return last.e
        }

        override fun remove() {
            remove(curr!!.previous!!.e)
        }
    }
}