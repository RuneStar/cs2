package org.runestar.cs2

import org.runestar.cs2.ir.Element

enum class EventProperty(val magic: Any, private val _type: Type) : Element {

    opbase("event_opbase", Type.STRING),
    mousex(Integer.MIN_VALUE + 1, Type.INT),
    mousey(Integer.MIN_VALUE + 2, Type.INT),
    com(Integer.MIN_VALUE + 3, Type.COMPONENT),
    opindex(Integer.MIN_VALUE + 4, Type.INT),
    comid(Integer.MIN_VALUE + 5, Type.INT),
    dragtarget(Integer.MIN_VALUE + 6, Type.COMPONENT),
    dragtargetid(Integer.MIN_VALUE + 7, Type.INT),
    keytyped(Integer.MIN_VALUE + 8, Type.KEY),
    keypressed(Integer.MIN_VALUE + 9, Type.CHAR),
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