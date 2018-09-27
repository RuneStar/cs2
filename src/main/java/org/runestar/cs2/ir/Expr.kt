package org.runestar.cs2.ir

import org.runestar.cs2.Type
import org.runestar.cs2.names
import java.lang.Math.abs
import java.lang.StringBuilder

internal interface Expr {

    var types: List<Type>
        get() = listOf(type)
        set(value) { type = value.single() }

    var type: Type
        get() = types.single()
        set(value) { types = listOf(value) }

    data class Cst(override var type: Type, val cst: Any?) : Expr {

        override fun toString(): String = cst.toString()
    }

    class Var(var id: Int, override var type: Type) : Expr {

        val name: String get() {
            val sb = StringBuilder()
            if (id < 0) sb.append('_')
            return sb.append(type.literal).append(abs(id)).toString()
        }

        override fun hashCode(): Int = type.topType.hashCode() xor id

        override fun equals(other: Any?): Boolean = other is Var && id == other.id && type.topType == other.type.topType

        override fun toString(): String = name
    }

    class Operation(
            override var types: List<Type>,
            val id: Int,
            val arguments: MutableList<Expr>
    ) : Expr {

        override fun toString(): String = "${names[id]}(${arguments.joinToString(", ")})"
    }
}