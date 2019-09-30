package org.runestar.cs2.ir

interface Element : Expression {

    val typing: Typing

    override val typings: List<Typing> get() = listOf(typing)

    class Constant(val value: Any?, override val typing: Typing) : Element {

        override fun toString() = "$value$typing"
    }

    class Variable(val varId: VarId, override val typing: Typing) : Element {

        override fun hashCode() = varId.hashCode()

        override fun equals(other: Any?) = other is Variable && varId == other.varId

        override fun toString() = "${varId.source}_${varId.id}$typing"
    }
}