package org.runestar.cs2

import org.runestar.cs2.bin.ParamTypeLoader
import org.runestar.cs2.bin.ScriptLoader
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {

    // interesting : 1320, 982, 20, 454, 170, 183, 1360, 1707, 1717

    val loadDir = Paths.get("scripts")
    val saveDir = Paths.get("cs2")
    Files.createDirectories(saveDir)

    val decompiler = Decompiler(
            ScriptLoader.Binary(loadDir, ""),
            ParamTypeLoader.StringSet(setOf(451, 452, 453, 454, 455, 456, 457, 458, 506, 510, 559))
    )

//    println(decompiler.decompile(1320, StringBuilder()))

    loadDir.toFile().list().forEach { fileName ->
        println(fileName)
        val s = decompiler.decompile(fileName.toInt(), StringBuilder()).toString()
        val saveFile = saveDir.resolve("$fileName.cs2")
        Files.write(saveFile, s.toByteArray())
    }
}