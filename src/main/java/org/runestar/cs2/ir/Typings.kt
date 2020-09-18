package org.runestar.cs2.ir

import org.runestar.cs2.bin.StackType

class Typings {

    val eventProperties = HashMap<EventProperty, Typing>()

    val constants = HashMap<Element, Typing>()

    val variables = HashMap<Variable, Typing>()

    val returns = HashMap<Int, List<Typing>>()

    val operations = HashMap<Expression, List<Typing>>()

    val prototypes = HashMap<Prototype, Typing>()

    val args = HashMap<Int, List<Typing>>()

    fun all(): List<Typing> = ArrayList<Typing>().apply {
        addAll(eventProperties.values)
        addAll(constants.values)
        addAll(variables.values)
        returns.values.forEach { addAll(it) }
        operations.values.forEach { addAll(it) }
        addAll(prototypes.values)
    }

    fun of(expression: Expression): List<Typing> {
        return when (expression) {
            is Expression.Compound -> expression.expressions.flatMap { of(it) }
            is Expression.Proc -> returns(expression.scriptId, expression.stackTypes)
            is Element -> listOf(of(expression))
            is Expression.ClientScript -> emptyList()
            is Expression.Operation -> operations.getOrPut(expression) { expression.stackTypes.map { Typing(it) } }
            else -> error(expression)
        }
    }

    fun of(element: Element): Typing {
        return when (element) {
            is Element.Variable -> of(element.variable)
            is Element.Constant -> constants.getOrPut(element) { Typing(element.stackType) }
            is EventProperty -> eventProperties.getOrPut(element) { Typing(element.prototype) }
            else -> error(element)
        }
    }

    fun of(prototype: Prototype): Typing {
        return prototypes.getOrPut(prototype) { Typing(prototype) }
    }

    fun of(prototypes: List<Prototype>): List<Typing> {
        return prototypes.map { of(it) }
    }

    fun of(variable: Variable): Typing {
        return variables.getOrPut(variable) { Typing(variable.stackType) }
    }

    fun args(scriptId: Int, stackTypes: List<StackType>): List<Typing> {
        val ts = args[scriptId]
        if (ts != null) return ts
        var i = 0
        var s = 0
        val ts2 = stackTypes.map { t ->
            val v = when (t) {
                StackType.INT -> Variable.int(scriptId, i++)
                StackType.STRING -> Variable.string(scriptId, s++)
            }
            of(v)
        }
        args[scriptId] = ts2
        return ts2
    }

    fun returns(scriptId: Int, stackTypes: List<StackType>): List<Typing> {
        return returns.getOrPut(scriptId) { stackTypes.map { Typing(it) } }
    }

    fun remove(variable: Variable) {
        remove(variables.remove(variable)!!)
    }

    fun remove(constant: Element.Constant) {
        remove(constants.remove(constant)!!)
    }
}