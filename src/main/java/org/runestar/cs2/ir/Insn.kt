package org.runestar.cs2.ir

internal interface Insn {

    interface Exprd : Insn {

        var expr: Expr
    }

    class Assignment(var definitions: List<Expr.Var>, override var expr: Expr) : Exprd {

        override fun toString(): String {
            val sb = StringBuilder()
            if (definitions.isNotEmpty()) {
                definitions.joinTo(sb, ", ")
                sb.append(" = ")
            }
            sb.append(expr)
            return sb.toString()
        }
    }

    class Return(override var expr: Expr) : Exprd {

        override fun toString() = expr.toString()
    }

    class Branch(override var expr: Expr, var pass: Label) : Exprd {

        override fun toString() = "if $expr goto $pass"
    }

    class Switch(override var expr: Expr, val map: Map<Int, Label>) : Exprd {

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("switch(").append(expr).append("):")
            for (e in map) {
                sb.append("\n\t\t").append(e.key).append(": ").append(e.value)
            }
            return sb.toString()
        }
    }

    data class Label(var id: Int) : Insn {

        override fun toString(): String = "@$id"
    }

    class Goto(var label: Label) : Insn {

        override fun toString(): String = "goto $label"
    }
}