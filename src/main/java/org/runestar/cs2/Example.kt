package org.runestar.cs2

import org.runestar.cs2.bin.Script
import org.runestar.cs2.cg.StrictGenerator
import org.runestar.cs2.util.Loader
import org.runestar.cs2.util.caching
import org.runestar.cs2.util.list
import org.runestar.cs2.util.withIds
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeSet

fun main() {
    writeReadme()
    decompile()
}

private fun writeReadme() {
    val sb = StringBuilder()
    sb.append("[![Discord](https://img.shields.io/discord/384870460640329728.svg?logo=discord)](https://discord.gg/G2kxrnU)\n\n")
    val scriptIds = Path.of("input").list().mapTo(TreeSet()) { it.toInt() }
    for (scriptId in scriptIds) {
        val scriptName = SCRIPT_NAMES.load(scriptId)
        if (scriptName == null) {
            sb.append("[**$scriptId**](scripts/script$scriptId.cs2)  \n")
        } else {
            sb.append("[**$scriptId**](scripts/$scriptName.cs2) `$scriptName`  \n")
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

    val generator = StrictGenerator { scriptName, script ->
        Files.writeString(saveDir.resolve("$scriptName.cs2"), script)
    }

    val scriptLoader = Loader { Script(Files.readAllBytes(loadDir.resolve(it.toString()))) }.caching()
    val scriptIds = loadDir.list().mapTo(TreeSet()) { it.toInt() }

    decompile(scriptLoader.withIds(scriptIds), generator)
}