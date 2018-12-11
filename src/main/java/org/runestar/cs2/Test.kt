package org.runestar.cs2

import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {

    val loadDir = Paths.get("input")
    val saveDir = Paths.get("scripts")
    Files.createDirectories(saveDir)

    val decompiler = Decompiler(Loader.Scripts(loadDir))

    loadDir.toFile().list().forEach { fileName ->
        val scriptId = fileName.toInt()
        val scriptName = Loader.SCRIPT_NAMES.load(scriptId) ?: fileName
        println("$scriptId $scriptName")
        val s = decompiler.decompile(scriptId)
        val saveFile = saveDir.resolve("$scriptName.cs2")
        Files.write(saveFile, s.toByteArray())
    }
}