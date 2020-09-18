package org.runestar.cs2.ir

import org.runestar.cs2.bin.Value

enum class EventProperty(val magic: Value, val prototype: Prototype) : Element {

    opbase(Value("event_opbase"), OPBASE),
    mousex(Value(Int.MIN_VALUE + 1), MOUSEX),
    mousey(Value(Int.MIN_VALUE + 2), MOUSEY),
    com(Value(Int.MIN_VALUE + 3), COMPONENT),
    opindex(Value(Int.MIN_VALUE + 4), OPINDEX),
    comsubid(Value(Int.MIN_VALUE + 5), COMSUBID),
    drop(Value(Int.MIN_VALUE + 6), DROP),
    dropsubid(Value(Int.MIN_VALUE + 7), DROPSUBID),
    key(Value(Int.MIN_VALUE + 8), KEY),
    keychar(Value(Int.MIN_VALUE + 9), KEYCHAR),
    ;

    override val stackType get() = prototype.stackType

    val literal = "event_$name"

    override fun toString() = literal

    companion object {

        private val VALUES = values().associateBy { it.magic }

        fun of(magic: Value): EventProperty? = VALUES[magic]
    }
}