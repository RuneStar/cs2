package org.runestar.cs2

import org.runestar.cs2.bin.ParamTypeLoader
import org.runestar.cs2.bin.ScriptLoader
import java.nio.file.Paths

fun main(args: Array<String>) {

    // interesting : 1320, 982, 20, 454, 170, 183, 1360, 1707, 1717

    val decompiler = Decompiler(
            ScriptLoader.Binary(Paths.get("scripts"), ""),
            ParamTypeLoader.IntOnly
    )

    println(decompiler.decompile(661, StringBuilder()))
}