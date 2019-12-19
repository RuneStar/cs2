package org.runestar.cs2.ir

import org.runestar.cs2.Alias
import org.runestar.cs2.Primitive
import org.runestar.cs2.Type
import org.runestar.cs2.Value

enum class EventProperty(val magic: Value, type: Type.Stackable) : Element {

    opbase(Value("event_opbase"), Primitive.STRING),
    mousex(Value(Integer.MIN_VALUE + 1), Primitive.INT),
    mousey(Value(Integer.MIN_VALUE + 2), Primitive.INT),
    com(Value(Integer.MIN_VALUE + 3), Primitive.COMPONENT),
    opindex(Value(Integer.MIN_VALUE + 4), Primitive.INT),
    comid(Value(Integer.MIN_VALUE + 5), Primitive.INT),
    dragtarget(Value(Integer.MIN_VALUE + 6), Primitive.COMPONENT),
    dragtargetid(Value(Integer.MIN_VALUE + 7), Primitive.INT),
    keypressed(Value(Integer.MIN_VALUE + 8), Alias.KEY),
    keytyped(Value(Integer.MIN_VALUE + 9), Primitive.CHAR),
    ;

    override val typing = Typing.to(type)

    val literal = "event_$name"

    override fun toString() = literal

    companion object {

        private val map = values().associateBy { it.magic }

        fun of(magic: Value): EventProperty? = map[magic]
    }
}