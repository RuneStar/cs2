package org.runestar.cs2.util

class PartitionedChain<E : Any>(
        private val delegate: Chain<E>,
        heads: List<E>
) : Iterable<PartitionedChain<E>.Block> {

    private val blocks = HashChain<Block>()

    private val map = HashMap<E, Block>()

    init {
        for (i in heads.indices) {
            val head: E = heads[i]
            val tail: E = if (i != heads.lastIndex) {
                delegate.previous(heads[i + 1])!!
            } else {
                delegate.last
            }
            blocks.addLast(Block(i, head, tail))
        }
        for (b in blocks) {
            for (e in b) {
                map[e] = b
            }
        }
    }

    override fun iterator(): Iterator<Block> = blocks.iterator()

    fun block(e: E): Block = map.getValue(e)

    val head: Block get() = blocks.first

    fun next(b: Block): Block = blocks.next(b)!!

    fun previous(b: Block): Block = blocks.previous(b)!!

    inner class Block(
            val index: Int,
            var head: E,
            var tail: E
    ) : Iterable<E> {

        override fun iterator() = delegate.iterator(head, tail)

        override fun toString() = index.toString()
    }
}