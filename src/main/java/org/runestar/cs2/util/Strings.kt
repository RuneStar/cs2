package org.runestar.cs2.util

fun String.strip(prefix: String, suffix: Char): String {
    require(startsWith(prefix)) { """"$this" must start with "$prefix"""" }
    require(endsWith(suffix)) { """"$this" must end with '$suffix'""" }
    return substring(prefix.length, length - 1)
}