package org.runestar.cs2.util

class PartitionedChain<E : Any>(
        chain: Chain<E>,
        heads: List<E>
) {

    val blocks: List<Block<E>>

    init {
        val ps = ArrayList<Block<E>>(heads.size)
        for (i in heads.indices) {
            val head: E = heads[i]
            val tail: E = if (i != heads.lastIndex) {
                chain.previous(heads[i + 1])!!
            } else {
                chain.last
            }
            ps.add(Block(chain, i, head, tail))
        }
        blocks = ps
    }

    fun of(e: E): Block<E> = blocks.first { e in it }

    class Block<E : Any>(
            val chain: Chain<E>,
            val index: Int,
            var head: E,
            var tail: E
    ) : Iterable<E> {

        fun prepend(e: E) {
            chain.insertBefore(e, head)
            head = e
        }

        fun append(e: E) {
            chain.insertAfter(e, tail)
            tail = e
        }

        fun remove(e: E) {
            if (e == head) {
                head = chain.next(head)!!
            } else if (e == tail) {
                tail = chain.previous(tail)!!
            }
            chain.remove(e)
        }

        override fun iterator() = iterator(head, tail)

        fun iterator(from: E, to: E) = chain.iterator(from, to)

        fun reverseIterator(from: E, to: E) = chain.reverseIterator(from, to)

        fun reverseIterator() = reverseIterator(tail, head)

        override fun toString(): String {
            return index.toString()
        }
    }
}