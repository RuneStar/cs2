package org.runestar.cs2.bin

sealed class Value {

    abstract val stackType: StackType

    data class Int(val int: kotlin.Int) : Value() {

        override val stackType get() = StackType.INT

        override fun toString() = int.toString()
    }

    data class String(val string: kotlin.String) : Value() {

        override val stackType get() = StackType.STRING

        override fun toString() = string
    }
}

fun Value(int: Int) = Value.Int(int)

fun Value(string: String) = Value.String(string)

val Value.int get() = (this as Value.Int).int

val Value.string get() = (this as Value.String).string

val Value.boolean get() = when (int) {
    0 -> false
    1 -> true
    else -> error(this)
}