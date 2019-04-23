package org.runestar.cs2.ir

interface Instruction {

    interface Evaluation : Instruction {

        var expression: Expression
    }

    class Assignment(var definitions: Expression, override var expression: Expression) : Evaluation {

        override fun toString(): String {
            val ds = definitions.list<Expression>()
            return if (ds.isEmpty()) {
                expression.toString()
            } else {
                "$definitions = $expression"
            }
        }
    }

    class Return(override var expression: Expression) : Evaluation {

        override fun toString() = "return($expression)"
    }

    class Branch(override var expression: Expression, var pass: Label) : Evaluation {

        override fun toString() = "if $expression goto $pass"
    }

    class Switch(override var expression: Expression, val map: Map<Int, Label>) : Evaluation {

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("switch(").append(expression).append("):")
            for (e in map) {
                sb.append("\n\t\t").append(e.key).append(": ").append(e.value)
            }
            return sb.toString()
        }
    }

    data class Label(var id: Int) : Instruction {

        override fun toString(): String = "@$id"
    }

    class Goto(var label: Label) : Instruction {

        override fun toString(): String = "goto $label"
    }
}