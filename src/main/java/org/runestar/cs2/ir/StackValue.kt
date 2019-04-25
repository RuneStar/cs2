package org.runestar.cs2.ir

import org.runestar.cs2.Type

internal data class StackValue(val value: Any?, val type: Type, val id: Int) {

    fun toExpression(type: Type = this.type) = Element.Variable.Stack(id, Type.merge(type, this.type))
}