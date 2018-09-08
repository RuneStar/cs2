package org.runestar.cs2

import org.runestar.cs2.raw.ScriptLoader
import org.runestar.cs2.ir1.Interpreter
import java.nio.file.Paths

fun main(args: Array<String>) {

    val sl = ScriptLoader.Binary(Paths.get("scripts"), "")
    val script = sl.load(21)
    println(script)

    val interpreter = Interpreter(sl)
    val func = interpreter.interpret(21)
    println(func)
}