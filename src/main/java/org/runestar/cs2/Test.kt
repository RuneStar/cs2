package org.runestar.cs2

import org.runestar.cs2.bin.ScriptLoader
import org.runestar.cs2.ir.*
import java.nio.file.Paths

fun main(args: Array<String>) {

    val sl = ScriptLoader.Binary(Paths.get("scripts"), "")
    val script = sl.load(7)
    println(script)

    val interpreter = Interpreter(sl)
    val func = interpreter.interpret(7)

    removeIdentityOperations(func)
    removeNopInstructions(func)
    println(func)

    replaceSingleUseAssignments(func)
    println(func)
}