package org.runestar.cs2.ir

import org.runestar.cs2.Type
import org.runestar.cs2.names

interface Expr {

    var types: List<Type>
        get() = listOf(type)
        set(value) { type = value.single() }

    var type: Type
        get() = types.single()
        set(value) { types = listOf(value) }

    data class Cst(override var type: Type, val cst: Any?) : Expr

    sealed class Variable : Expr {

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

        override fun toString(): String = "${javaClass.simpleName}($id)"
    }

    class Operation(
            override var types: List<Type>,
            val id: Int,
            val arguments: MutableList<Expr>
    ) : Expr {

        override fun toString(): String = "${names[id]}(${arguments.joinToString(", ")})"
    }
}