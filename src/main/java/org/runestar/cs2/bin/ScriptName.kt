package org.runestar.cs2.bin

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
        ScriptName(Trigger.of(n and 0xFF), (n shr 8).toString())
    }
}