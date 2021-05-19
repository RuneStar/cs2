package org.runestar.cs2.ir

import org.runestar.cs2.bin.StackType

interface Variable {

    val id: Int

    val stackType: StackType

    interface OfInt : Variable {

        override val stackType get() = StackType.INT
    }

    interface OfString : Variable {

        override val stackType get() = StackType.STRING
    }

    interface Global : Variable

    interface Local : Variable {

        val script: Int
    }

    interface Stack : Local

    abstract class Base : Variable {

        final override fun toString() = javaClass.simpleName + id.toString()
    }

    data class varp(override val id: Int) : Base(), Global, OfInt

    data class varbit(override val id: Int) : Base(), Global, OfInt

    data class varcint(override val id: Int) : Base(), Global, OfInt

    data class varcstring(override val id: Int) : Base(), Global, OfString

    data class stackint(override val script: Int, override val id: Int) : Base(), Stack, OfInt

    data class stackstring(override val script: Int, override val id: Int) : Base(), Stack, OfString

    data class int(override val script: Int, override val id: Int) : Base(), Local, OfInt

    data class string(override val script: Int, override val id: Int) : Base(), Local, OfString

    data class array(override val script: Int, override val id: Int) : Base(), Local, OfInt

    data class varclansetting(override val id: Int) : Base(), Global, OfInt

    data class varclan(override val id: Int) : Base(), Global, OfInt
}