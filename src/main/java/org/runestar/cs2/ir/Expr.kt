package org.runestar.cs2.ir

import org.runestar.cs2.TopType
import org.runestar.cs2.names

interface Expr {

    data class Const(val cst: Any?) : Expr {

        override fun toString(): String = cst.toString()
    }

    data class Var(val name: String, val type: TopType) : Expr {

        override fun toString(): String = name

        companion object {

            fun si(index: Int) = Var("i$index", TopType.INT)

            fun li(index: Int) = Var("j$index", TopType.INT)

            fun ss(index: Int) = Var("s$index", TopType.STRING)

            fun ls(index: Int) = Var("z$index", TopType.STRING)
        }
    }

    class Operation(val id: Int, val arguments: MutableList<Expr>) : Expr {

        override fun toString(): String = "${names[id]}(${arguments.joinToString(", ")})"
    }
}