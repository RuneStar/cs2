package org.runestar.cs2.util

import java.lang.invoke.MethodHandles
import java.nio.charset.Charset
import java.nio.file.Path

fun Short.toUnsignedInt(): Int = java.lang.Short.toUnsignedInt(this)

fun Byte.toUnsignedInt(): Int = java.lang.Byte.toUnsignedInt(this)

val CP1252 = charset("windows-1252")

fun Char.toByte(charset: Charset): Byte = toString().toByteArray(charset).single()

fun counter(): () -> Int {
    var i = 0
    return { i++ }
}

inline val thisClass: Class<*> get() = MethodHandles.lookup().lookupClass()

fun Path.list(): List<String> = checkNotNull(toFile().list()).asList()