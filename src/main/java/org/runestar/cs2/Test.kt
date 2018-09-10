package org.runestar.cs2

import org.runestar.cs2.bin.ScriptLoader
import org.runestar.cs2.ir.removeIdentityOperations
import org.runestar.cs2.ir.removeNopInstructions
import org.runestar.cs2.ir.Interpreter
import org.runestar.cs2.ir.replaceTrivialAssignments
import java.nio.file.Paths

fun main(args: Array<String>) {

    val sl = ScriptLoader.Binary(Paths.get("scripts"), "")
    val script = sl.load(3)
    println(script)

    val interpreter = Interpreter(sl)
    val func = interpreter.interpret(3)
    println(func)

    removeIdentityOperations(func)
    removeNopInstructions(func)
    replaceTrivialAssignments(func)
    println(func)
}