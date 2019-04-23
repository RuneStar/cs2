package org.runestar.cs2.ir

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

    class Operation(
            override var types: List<Type>,
            val id: Int,
            var arguments: Expression
    ) : Expression {

        override fun toString(): String = "${names[id]}($arguments)"
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