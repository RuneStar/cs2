package org.runestar.cs2.bin

import kotlin.math.abs

data class ScriptName(val trigger: Trigger, val name: String) {

    override fun toString() = "[$trigger,$name]"
}

fun ScriptName(cacheName: String): ScriptName {
    val n = cacheName.toIntOrNull()
    return if (n == null) {
        val comma = cacheName.indexOf(',')
        require(cacheName.startsWith('[') && cacheName.endsWith(']') && comma != -1)
        ScriptName(Trigger.valueOf(cacheName.substring(1, comma)), cacheName.substring(comma + 1, cacheName.length - 1))
    } else {
        var t = n + 512
        if (t in 10..50) {
            // global trigger
            ScriptName(Trigger.of(t), "_")
        } else {
            val c = abs((n shl 8) - 3)
            t = (c shr 8) + n + 768
            if (t in 0..255) {
                // category trigger
                ScriptName(Trigger.of(n + 512), "_category_$c")
            } else {
                // type trigger
                ScriptName(Trigger.of(n and 0xFF), (n shr 8).toString())
            }
        }
    }
}