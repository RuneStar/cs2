package org.runestar.cs2

interface Value {

    val type: StackType

    val int: Int

    val string: String
}

private data class ValueInt(override val int: Int) : Value {
    override val type get() = StackType.INT
    override val string: String get() = throw UnsupportedOperationException()
}

private data class ValueString(override val string: String) : Value {
    override val type get() = StackType.STRING
    override val int: Int get() = throw UnsupportedOperationException()
}

fun Value(int: Int): Value = ValueInt(int)

fun Value(string: String): Value = ValueString(string)