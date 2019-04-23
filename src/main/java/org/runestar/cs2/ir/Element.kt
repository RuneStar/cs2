package org.runestar.cs2.ir

import org.runestar.cs2.Type

interface Element : Expression {

    var type: Type

    override var types: List<Type>
        get() = listOf(type)
        set(value) { type = value.single() }

    data class Constant(override var type: Type, val value: Any?) : Element {

        override fun toString(): String = "Constant($value, $type)"
    }

    sealed class Variable : Element {

        abstract var id: Int

        class Stack(override var id: Int, override var type: Type) : Variable()

        class Local(override var id: Int, override var type: Type) : Variable()

        class Varp(override var id: Int, override var type: Type) : Variable()

        class Varbit(override var id: Int, override var type: Type) : Variable()

        class Varc(override var id: Int, override var type: Type) : Variable()

        override fun hashCode(): Int = id xor type.topType.hashCode()

        override fun equals(other: Any?): Boolean = other is Variable &&
                javaClass == other.javaClass &&
                id == other.id &&
                type.topType == other.type.topType

        override fun toString(): String = "${javaClass.simpleName}($id, $type)"
    }
}