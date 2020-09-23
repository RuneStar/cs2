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
    val readme = StringBuilder()
    readme.append("[![Discord](https://img.shields.io/discord/384870460640329728.svg?logo=discord)](https://discord.gg/G2kxrnU)\n\n")

    val loadDir = Path.of("input")
    val saveDir = Path.of("scripts", "scripts")
    Files.createDirectories(saveDir)

    val generator = StrictGenerator { scriptId, scriptName, script ->
        Files.writeString(saveDir.resolve("$scriptName.cs2"), script)
        if (SCRIPT_NAMES.load(scriptId) != null) {
            readme.append("[**$scriptId**](scripts/$scriptName.cs2) `$scriptName`  \n")
        } else {
            readme.append("[**$scriptId**](scripts/$scriptName.cs2)  \n")
        }
    }

    val scriptLoader = Loader { Script(Files.readAllBytes(loadDir.resolve(it.toString()))) }.caching()
    val scriptIds = loadDir.list().mapTo(TreeSet()) { it.toInt() }

    decompile(scriptLoader.withIds(scriptIds), generator)

    Files.writeString(Path.of("scripts", "README.md"), readme)
}