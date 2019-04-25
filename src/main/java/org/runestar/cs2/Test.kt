package org.runestar.cs2

import org.runestar.cs2.cg.StrictGenerator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeSet
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
    for (scriptId in File("input").list().mapTo(TreeSet()) { it.toInt() }) {
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
    Files.writeString(saveFile, sb)
}

private fun decompile() {
    val loadDir = Path.of("input")
    val saveDir = Path.of("scripts", "scripts")
    Files.createDirectories(saveDir)

    val io = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    val generator = StrictGenerator { scriptName, script ->
        println(scriptName)
        io.submit { Files.writeString(saveDir.resolve("$scriptName.cs2"), script) }
    }

    val scriptLoader = Loader { Script.read(Files.readAllBytes(loadDir.resolve(it.toString()))) }.caching()
    val scriptIds = loadDir.toFile().list().mapTo(TreeSet()) { it.toInt() }

    decompile(scriptLoader.withKeys(scriptIds), generator)

    io.shutdown()
    io.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS)
}