package org.runestar.cs2.ir

import org.runestar.cs2.Opcodes
import org.runestar.cs2.names

interface Expression {

    val typings: List<Typing>

    val precedence get() = 0

    class Compound(val expressions: List<Expression>) : Expression {

        override val typings: List<Typing> get() = expressions.flatMap { it.typings }

        override fun toString(): String = expressions.joinToString(", ")
    }

    open class Operation(
            override var typings: List<Typing>,
            val id: Int,
            var arguments: Expression
    ) : Expression {

        override fun toString(): String = "${names[id]}($arguments)$typings"

        interface Scripted {

            val scriptId: Int

            val scriptArguments: Expression
        }

        class Invoke(
                types: List<Typing>,
                override val scriptId: Int,
                arguments: Expression
        ) : Operation(types, Opcodes.GOSUB_WITH_PARAMS, arguments), Scripted {

            override val scriptArguments: Expression get() = arguments

            override fun toString(): String = "~$scriptId($arguments)$typings"
        }

        class AddHook(
                id: Int,
                override val scriptId: Int,
                arguments: Expression
        ) : Operation(emptyList(), id, arguments), Scripted {

            override val scriptArguments: Expression get() {
                val args = arguments.list<Expression>()
                val triggerCount = (args[args.size - 2] as Element.Constant).value.int
                return Expression(args.subList(0, args.size - triggerCount - 2))
            }
        }

        override val precedence
            get() = when (id) {
                // Calc ops
                Opcodes.MULTIPLY, Opcodes.DIV, Opcodes.MOD -> 8
                Opcodes.ADD, Opcodes.SUB -> 7
                // Branch ops
                Opcodes.BRANCH_GREATER_THAN, Opcodes.BRANCH_GREATER_THAN_OR_EQUALS,
                Opcodes.BRANCH_LESS_THAN, Opcodes.BRANCH_LESS_THAN_OR_EQUALS -> 6
                Opcodes.BRANCH_EQUALS, Opcodes.BRANCH_NOT -> 5
                Opcodes.AND -> 4
                Opcodes.OR -> 3
                Opcodes.SS_AND -> 2
                Opcodes.SS_OR -> 1
                else -> super.precedence
            }
    }
}

fun Expression(): Expression = Expression.Compound(emptyList())

fun Expression(expression: Expression): Expression = error(expression)

fun Expression(vararg expressions: Expression): Expression = Expression.Compound(expressions.asList())

fun Expression(expressions: List<Expression>): Expression = expressions.singleOrNull() ?: Expression.Compound(expressions)

operator fun Expression.plus(other: Expression): Expression = Expression(list<Expression>() + other.list())

inline fun <reified T : Expression> Expression.list(): List<T> {
    return when (this) {
        is Expression.Compound -> expressions.map { it as T }
        else -> listOf(this as T)
    }
}