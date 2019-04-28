package org.runestar.cs2.ir

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type
import org.runestar.cs2.names

interface Expression {

    var types: List<Type>

    class Compound(val expressions: List<Expression>) : Expression {

        override var types: List<Type>
            get() = expressions.flatMap { it.types }
            set(value) {
                var i = 0
                for (e in expressions) {
                    val ts = e.types
                    e.types = value.subList(i, i + ts.size)
                    i += ts.size
                }
            }

        override fun toString(): String = expressions.joinToString(", ")
    }

    open class Operation(
            override var types: List<Type>,
            val id: Int,
            var arguments: Expression
    ) : Expression {

        override fun toString(): String = "${names[id]}($arguments)($types)"

        interface Scripted {

            val scriptId: Int

            val scriptArguments: Expression
        }

        class Invoke(
                types: List<Type>,
                override val scriptId: Int,
                arguments: Expression
        ) : Operation(types, Opcodes.INVOKE, arguments), Scripted {

            override val scriptArguments: Expression get() = arguments

            override fun toString(): String = "~$scriptId($arguments)($types)"
        }

        class AddHook(
                id: Int,
                override val scriptId: Int,
                arguments: Expression
        ) : Operation(emptyList(), id, arguments), Scripted {

            override val scriptArguments: Expression get() {
                val args = arguments.list<Expression>()
                val triggerCount = (args[args.size - 2] as Element.Constant).value as Int
                return Expression(args.subList(0, args.size - triggerCount - 2))
            }
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