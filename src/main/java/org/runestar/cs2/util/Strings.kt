package org.runestar.cs2.util

fun String.trim(prefix: String, suffix: Char): String {
    require(startsWith(prefix) && endsWith(suffix))
    return substring(prefix.length, length - 1)
}