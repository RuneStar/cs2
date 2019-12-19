package org.runestar.cs2.ir

import org.runestar.cs2.Type
import org.runestar.cs2.Value

interface Element : Expression {

    val typing: Typing

    override val typings: List<Typing> get() = listOf(typing)

    class Constant(val value: Value, override val typing: Typing) : Element {

        override fun toString() = "$value$typing"
    }

    class Variable(val varId: VarId, override val typing: Typing, val value: Value? = null) : Element {

        init {
            if (value != null) check(value.type == typing.stackType)
        }

        override fun hashCode() = varId.hashCode()

        override fun equals(other: Any?) = other is Variable && varId == other.varId

        override fun toString() = "${varId.source}_${varId.id}$typing"
    }
}

fun Value.asConstant(type: Type.Stackable? = null) = Element.Constant(this, Typing.to(type))