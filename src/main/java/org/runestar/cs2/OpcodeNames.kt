package org.runestar.cs2

val names: Map<Int, String> = Opcodes::class.java.declaredFields.associate { it.getInt(null) to it.name.toLowerCase() }