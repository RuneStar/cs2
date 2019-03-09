package org.runestar.cs2

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main() {
    writeReadme()
    decompile()
}

private fun writeReadme() {
    val sb = StringBuilder()
    val prefix = "scripts/"
    sb.append("[![Discord](https://img.shields.io/discord/384870460640329728.svg?logo=discord)](https://discord.gg/G2kxrnU)\n\n")
    val ids = File("input").list().map { it.toInt() }.sorted()
    ids.forEach { scriptId ->
        val scriptName = Loader.SCRIPT_NAMES.load(scriptId)
        if (scriptName == null) {
            val link = "${prefix}script$scriptId.cs2"
            sb.append("[**$scriptId**]($link)  \n")
        } else {
            val link = "$prefix$scriptName.cs2"
            sb.append("[**$scriptId**]($link) `$scriptName`  \n")
        }
    }
    val saveFile = Path.of("scripts", "README.md")
    Files.createDirectories(saveFile.parent)
    Files.write(saveFile, sb.toString().toByteArray())
}

private fun decompile() {
    val loadDir = Path.of("input")
    val saveDir = Path.of("scripts", "scripts")
    Files.createDirectories(saveDir)
    val decompiler = Decompiler(Loader.Scripts(loadDir))
    val io = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    loadDir.toFile().list().forEach { fileName ->
        val scriptId = fileName.toInt()
        val scriptName = Loader.SCRIPT_NAMES.load(scriptId) ?: "script$fileName"
        println("$scriptId $scriptName")
        val decompiled = decompiler.decompile(scriptId)
        val saveFile = saveDir.resolve("$scriptName.cs2")
        io.submit {
            Files.writeString(saveFile, decompiled)
        }
    }

    io.shutdown()
    io.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS)
}