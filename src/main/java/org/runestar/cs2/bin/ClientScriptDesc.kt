package org.runestar.cs2.bin

import org.runestar.cs2.util.CP1252

data class ClientScriptDesc(
        val argumentTypes: List<Type>,
        val triggers: Boolean,
)

fun ClientScriptDesc(desc: String): ClientScriptDesc {
    val bytes = desc.toByteArray(CP1252)
    val triggers = bytes.isNotEmpty() && desc.last() == 'Y'
    val argTypes = ArrayList<Type>(bytes.size)
    for (i in bytes.indices) {
        if (!triggers || i != bytes.lastIndex) {
            argTypes.add(Type.of(bytes[i]))
        }
    }
    return ClientScriptDesc(argTypes, triggers)
}