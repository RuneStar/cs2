package org.runestar.cs2

// lowercase
val names: Map<Int, String> = Opcodes::class.java.declaredFields.associate { it.getInt(null) to it.name.toLowerCase() }

// uppercase
val namesReverse: Map<String, Int> = Opcodes::class.java.declaredFields.associate { it.name to it.getInt(null) }