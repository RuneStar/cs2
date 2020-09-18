package org.runestar.cs2.ir

import org.runestar.cs2.bin.*
import org.runestar.cs2.bin.StackType

interface Expression {

    val stackTypes: List<StackType>

    class Compound(val expressions: List<Expression>) : Expression {

        init {
            require(expressions.none { it is Compound })
            require(expressions.size != 1)
        }

        override val stackTypes get() = expressions.flatMap { it.stackTypes }

        override fun toString() = expressions.joinToString(",", "(", ")")
    }

    class Operation(
            override val stackTypes: List<StackType>,
            override val opcode: Int,
            var arguments: Expression,
            val dot: Boolean = false,
    ) : Command {

        override fun toString() = buildString {
            if (dot) append('.')
            append(opcodeName).append(' ').append(arguments)
        }
    }

    interface Command : Expression {

        val opcode: Int

        val opcodeName: String get() = opcodeNames.getValue(opcode)
    }

    interface Script : Command {

        val scriptId: Int

        val arguments: Expression
    }

    class Proc(
            override val stackTypes: List<StackType>,
            override val scriptId: Int,
            override var arguments: Expression,
    ) : Script {

        override val opcode get() = GOSUB_WITH_PARAMS

        override fun toString() = "~$scriptId $arguments"
    }

    class ClientScript(
            override val opcode: Int,
            override val scriptId: Int,
            override var arguments: Expression,
            var triggers: Expression,
            val dot: Boolean,
            var component: Expression?,
    ) : Script {

        override val stackTypes get() = emptyList<StackType>()

        override fun toString() = buildString {
            if (dot) append('.')
            append(opcodeName).append(' ')
            append(scriptId).append(' ')
            append(arguments).append(' ')
            append(triggers)
            if (component != null) append(' ').append(component)
        }
    }
}

fun Expression(): Expression = Expression.Compound(emptyList())

fun Expression(expression: Expression): Expression = error(expression)

fun Expression(vararg expressions: Expression): Expression = Expression.Compound(expressions.asList())

fun Expression(expressions: List<Expression>): Expression = expressions.singleOrNull() ?: Expression.Compound(expressions)

operator fun Expression.plus(other: Expression): Expression = Expression(asList + other.asList)

val Expression.asList: List<Expression> get() = when (this) {
    is Expression.Compound -> expressions
    else -> listOf(this)
}