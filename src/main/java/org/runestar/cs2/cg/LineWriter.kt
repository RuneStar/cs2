package org.runestar.cs2.cg

class LineWriter(
        private val buf: StringBuilder,
        private val indent: String = "\t",
        private val lineSeparator: String = "\n"
) : Appendable {

    var indents = 0

    override fun append(c: Char): LineWriter = apply { buf.append(c) }

    fun append(n: Int): LineWriter = apply { buf.append(n) }

    fun append(b: Boolean): LineWriter = apply { buf.append(b) }

    fun append(str: String?): LineWriter = apply { buf.append(str) }

    override fun append(csq: CharSequence?): LineWriter = apply { buf.append(csq) }

    override fun append(csq: CharSequence?, start: Int, end: Int): LineWriter = apply { buf.append(csq, start, end) }

    fun nextLine(): LineWriter = apply {
        append(lineSeparator)
        repeat(indents) { append(indent) }
    }
}