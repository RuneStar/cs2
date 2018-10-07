package org.runestar.cs2.cg

class LineWriter(
        private val appendable: Appendable,
        private val indent: CharSequence = "\t",
        private val lineSeparator: CharSequence = "\n"
) : Appendable {

    var indents = 0

    override fun append(c: Char): LineWriter = apply { appendable.append(c) }

    override fun append(csq: CharSequence?): LineWriter = apply { appendable.append(csq) }

    override fun append(csq: CharSequence?, start: Int, end: Int): LineWriter = apply { appendable.append(csq, start, end) }

    private fun appendIndents(): LineWriter = apply { repeat(indents) { append(indent) } }

    private fun appendLineSeparator(): LineWriter = apply { append(lineSeparator) }

    fun nextLine(): LineWriter = apply { appendLineSeparator().appendIndents() }
}