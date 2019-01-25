package org.runestar.cs2

import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main() {
    val start = Instant.now()

    val loadDir = Paths.get("input")
    val saveDir = Paths.get("scripts")
    Files.createDirectories(saveDir)
    val decompiler = Decompiler(Loader.Scripts(loadDir))
    val io = Executors.newCachedThreadPool()

    loadDir.toFile().list().forEach { fileName ->
        val scriptId = fileName.toInt()
        val scriptName = Loader.SCRIPT_NAMES.load(scriptId) ?: "script$fileName"
        println("$scriptId $scriptName")
        val decompiled = decompiler.decompile(scriptId)
        val saveFile = saveDir.resolve("$scriptName.cs2")
        io.submit {
            Files.write(saveFile, decompiled.toByteArray())
        }
    }

    io.shutdown()
    io.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS)

    println(Duration.between(start, Instant.now()))
}