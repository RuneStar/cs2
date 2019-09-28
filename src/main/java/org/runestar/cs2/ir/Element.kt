package org.runestar.cs2.ir

import org.runestar.cs2.Primitive
import org.runestar.cs2.Type

interface Element : Expression {

    var type: Type

    override var types: List<Type>
        get() = listOf(type)
        set(value) { type = value.single() }

    data class Constant(val value: Any?, override var type: Type) : Element {

        constructor(value: Int) : this(value, Primitive.INT)
    }

    data class Variable(
            val source: VarSource,
            val id: Int,
            override var type: Type
    ) : Element {

        override fun hashCode(): Int = id xor source.hashCode()

        override fun equals(other: Any?): Boolean = other is Variable &&
                source == other.source &&
                id == other.id
    }
}