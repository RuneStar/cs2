package org.runestar.cs2

import org.runestar.cs2.bin.NameLoader
import org.runestar.cs2.bin.ScriptLoader
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {

    val loadDir = Paths.get("input")
    val saveDir = Paths.get("scripts")
    Files.createDirectories(saveDir)

    val decompiler = Decompiler(ScriptLoader.Binary(loadDir, ""))

    loadDir.toFile().list().forEach { fileName ->
        val scriptId = fileName.toInt()
        val scriptName = NameLoader.SCRIPTS.load(scriptId) ?: fileName
        println("$scriptId $scriptName")
        val s = decompiler.decompile(scriptId)
        val saveFile = saveDir.resolve("$scriptName.cs2")
        Files.write(saveFile, s.toByteArray())
    }
}