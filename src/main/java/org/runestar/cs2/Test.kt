package org.runestar.cs2

import org.runestar.cs2.bin.NameLoader
import org.runestar.cs2.bin.ParamTypeLoader
import org.runestar.cs2.bin.ScriptLoader
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {

    val loadDir = Paths.get("input")
    val saveDir = Paths.get("scripts")
    Files.createDirectories(saveDir)

    val decompiler = Decompiler(
            ScriptLoader.Binary(loadDir, ""),
            ParamTypeLoader.StringSet(setOf(451, 452, 453, 454, 455, 456, 457, 458, 506, 510, 559, 595)),
            NameLoader.FONTS,
            NameLoader.GRAPHICS,
            NameLoader.SCRIPTS
    )

    loadDir.toFile().list().forEach { fileName ->
        val scriptId = fileName.toInt()
        val s = decompiler.decompile(scriptId)
        val scriptName = NameLoader.SCRIPTS.load(scriptId) ?: fileName
        println(scriptName)
        val saveFile = saveDir.resolve("$scriptName.cs2")
        Files.write(saveFile, s.toByteArray())
    }
}