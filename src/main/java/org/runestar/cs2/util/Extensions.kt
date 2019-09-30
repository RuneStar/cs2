package org.runestar.cs2.util

import java.nio.charset.Charset

fun Short.toUnsignedInt(): Int = java.lang.Short.toUnsignedInt(this)

fun Byte.toUnsignedInt(): Int = java.lang.Byte.toUnsignedInt(this)

fun <T> MutableCollection<T>.removeFirst(): T {
    val itr = iterator()
    return itr.next().also { itr.remove() }
}

@JvmField val CP1252 = charset("windows-1252")

fun Char.toByte(charset: Charset): Byte = toString().toByteArray(charset).single()