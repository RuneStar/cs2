package org.runestar.cs2

data class ScriptName(val trigger: Trigger, val name: String) {

    override fun toString() = "[$trigger,$name]"

    companion object {

        fun of(cacheName: String): ScriptName {
            val n = cacheName.toIntOrNull()
            return if (n == null) {
                require(cacheName.startsWith('[') && cacheName.endsWith(']'))
                val comma = cacheName.indexOf(',')
                ScriptName(Trigger.valueOf(cacheName.substring(1, comma)), cacheName.substring(comma + 1, cacheName.length - 1))
            } else {
                ScriptName(Trigger.of(n and 0xFF), (n shr 8).toString())
            }
        }
    }
}