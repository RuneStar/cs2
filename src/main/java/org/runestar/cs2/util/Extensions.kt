package org.runestar.cs2.util

import java.net.URL

fun Short.toUnsignedInt(): Int = java.lang.Short.toUnsignedInt(this)

fun Byte.toUnsignedInt(): Int = java.lang.Byte.toUnsignedInt(this)

fun String.strip(prefix: String, suffix: Char): String {
    require(startsWith(prefix)) { """"$this" must start with "$prefix"""" }
    require(endsWith(suffix)) { """"$this" must end with '$suffix'""" }
    return substring(prefix.length, length - 1)
}

fun <T> MutableCollection<T>.removeFirst(): T {
    val itr = iterator()
    return itr.next().also { itr.remove() }
}

inline fun URL.forEachLine(action: (String) -> Unit) {
    openStream().bufferedReader().use {
        while (true) {
            action(it.readLine() ?: return@use)
        }
    }
}