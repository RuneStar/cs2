package org.runestar.cs2.ir

import org.runestar.cs2.Type
import org.runestar.cs2.names

interface Expr {

    val types: List<Type>

    data class Cst(val type: Type, val cst: Any?) : Expr {

        override val types: List<Type> get() = listOf(type)

        override fun toString(): String = cst.toString()
    }

    data class Var(val name: String, val type: Type) : Expr {

        override val types: List<Type> get() = listOf(type)

        override fun toString(): String = name

        companion object {

            fun li(index: Int) = Var("i$index", Type.INT)

            fun ls(index: Int) = Var("s$index", Type.STRING)
        }
    }

    class Operation(
            override val types: List<Type>,
            val id: Int,
            val arguments: MutableList<Expr>
    ) : Expr {

        override fun toString(): String = "${names[id]}(${arguments.joinToString(", ")})"
    }
}