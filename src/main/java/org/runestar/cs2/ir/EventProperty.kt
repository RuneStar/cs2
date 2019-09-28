package org.runestar.cs2.ir

import org.runestar.cs2.Primitive
import org.runestar.cs2.Primitive.*
import org.runestar.cs2.Type

enum class EventProperty(val magic: Any, private val _type: Primitive) : Element {

    opbase("event_opbase", STRING),
    mousex(Integer.MIN_VALUE + 1, INT),
    mousey(Integer.MIN_VALUE + 2, INT),
    com(Integer.MIN_VALUE + 3, COMPONENT),
    opindex(Integer.MIN_VALUE + 4, INT),
    comid(Integer.MIN_VALUE + 5, INT),
    dragtarget(Integer.MIN_VALUE + 6, COMPONENT),
    dragtargetid(Integer.MIN_VALUE + 7, INT),
    keypressed(Integer.MIN_VALUE + 8, KEY),
    keytyped(Integer.MIN_VALUE + 9, CHAR),
    ;

    override var type: Type
        get() = _type
        set(value) = check(value == _type)

    val literal = "event_$name"

    companion object {

        private val map = values().associateBy { it.magic }

        fun of(magic: Any): EventProperty? = map[magic]
    }
}