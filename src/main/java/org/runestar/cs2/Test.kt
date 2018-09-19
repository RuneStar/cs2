package org.runestar.cs2

import org.runestar.cs2.bin.ScriptLoader
import java.nio.file.Paths

fun main(args: Array<String>) {

    // interesting : 1320, 982, 20, 454, 170, 183

    val decompiler = Decompiler(ScriptLoader.Binary(Paths.get("scripts"), ""))
    println(decompiler.decompile(982, StringBuilder()))
}