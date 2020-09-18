package org.runestar.cs2.ir

interface Instruction {

    interface Evaluation : Instruction {

        var expression: Expression
    }

    class Assignment(
            var definitions: Expression,
            override var expression: Expression,
    ) : Evaluation {

        constructor(expression: Expression) : this(Expression(), expression)

        init {
            check(definitions.stackTypes.size == expression.stackTypes.size || definitions.stackTypes.isEmpty())
        }

        override fun toString() = "$definitions = $expression"
    }

    class Return(override var expression: Expression) : Evaluation {

        override fun toString() = "return $expression"
    }

    class Branch(
            override var expression: Expression,
            var pass: Label,
    ) : Evaluation {

        override fun toString() = "if $expression goto $pass"
    }

    class Switch(
            override var expression: Expression,
            val cases: Map<Int, Label>,
    ) : Evaluation {

        override fun toString() = buildString {
            append("switch ").append(expression).append(" ")
            cases.entries.joinTo(this, ",") { "${it.key}:${it.value}" }
        }
    }

    data class Label(var id: Int) : Instruction {

        override fun toString() = "@$id"
    }

    class Goto(var label: Label) : Instruction {

        override fun toString() = "goto $label"
    }
}