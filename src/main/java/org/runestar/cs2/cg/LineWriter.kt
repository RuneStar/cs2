package org.runestar.cs2.cg

class LineWriter(
        private val appendable: StringBuilder,
        private val indent: String = "\t",
        private val lineSeparator: String = "\n"
) : Appendable {

    var indents = 0

    override fun append(c: Char): LineWriter = apply { appendable.append(c) }

    fun append(n: Int): LineWriter = apply { appendable.append(n) }

    fun append(b: Boolean): LineWriter = apply { appendable.append(b) }

    fun append(str: String?): LineWriter = apply { appendable.append(str) }

    override fun append(csq: CharSequence?): LineWriter = apply { appendable.append(csq) }

    override fun append(csq: CharSequence?, start: Int, end: Int): LineWriter = apply { appendable.append(csq, start, end) }

    fun nextLine(): LineWriter = apply {
        append(lineSeparator)
        repeat(indents) { append(indent) }
    }
}