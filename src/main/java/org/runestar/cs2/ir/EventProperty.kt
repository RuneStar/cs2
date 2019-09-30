package org.runestar.cs2.ir

import org.runestar.cs2.Alias
import org.runestar.cs2.Primitive
import org.runestar.cs2.Type

enum class EventProperty(val magic: Any, type: Type.Stackable) : Element {

    opbase("event_opbase", Primitive.STRING),
    mousex(Integer.MIN_VALUE + 1, Primitive.INT),
    mousey(Integer.MIN_VALUE + 2, Primitive.INT),
    com(Integer.MIN_VALUE + 3, Primitive.COMPONENT),
    opindex(Integer.MIN_VALUE + 4, Primitive.INT),
    comid(Integer.MIN_VALUE + 5, Primitive.INT),
    dragtarget(Integer.MIN_VALUE + 6, Primitive.COMPONENT),
    dragtargetid(Integer.MIN_VALUE + 7, Primitive.INT),
    keypressed(Integer.MIN_VALUE + 8, Alias.KEY),
    keytyped(Integer.MIN_VALUE + 9, Primitive.CHAR),
    ;

    override val typing = Typing.to(type)

    val literal = "event_$name"

    override fun toString() = literal

    companion object {

        private val map = values().associateBy { it.magic }

        fun of(magic: Any): EventProperty? = map[magic]
    }
}