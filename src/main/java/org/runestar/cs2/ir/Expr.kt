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

    data class Cst(override var type: Type, val cst: Any?) : Expr {

        override fun toString(): String = cst.toString()
    }

    class Var(val name: String, override var type: Type) : Expr {

        override fun hashCode(): Int = name.hashCode()

        override fun equals(other: Any?): Boolean = other is Var && name == other.name

        override fun toString(): String = "(${type.literal})$name"

        companion object {
            fun l(index: Int, type: Type) = Var("${type.topType.type.desc}$index", type)
        }
    }

    class Operation(
            override var types: List<Type>,
            val id: Int,
            val arguments: MutableList<Expr>
    ) : Expr {

        override fun toString(): String = "${names[id]}(${arguments.joinToString(", ")})"
    }
}