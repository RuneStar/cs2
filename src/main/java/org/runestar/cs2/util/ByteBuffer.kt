package org.runestar.cs2.util

import java.nio.ByteBuffer

fun Short.toUnsignedInt(): Int = java.lang.Short.toUnsignedInt(this)

fun Byte.toUnsignedInt(): Int = java.lang.Byte.toUnsignedInt(this)

@JvmField val CHARSET = charset("windows-1252")

fun ByteBuffer.readString(): String {
    val origPos = position()
    var length = 0
    while (get() != 0.toByte()) length++
    if (length == 0) return ""
    val byteArray = ByteArray(length)
    position(origPos)
    get(byteArray)
    position(position() + 1)
    return String(byteArray, CHARSET)
}