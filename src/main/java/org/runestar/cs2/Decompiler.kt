package org.runestar.cs2

import org.runestar.cs2.bin.ParamTypeLoader
import org.runestar.cs2.bin.ScriptLoader
import org.runestar.cs2.cg.write
import org.runestar.cs2.ir.Interpreter

class Decompiler(
        scriptLoader: ScriptLoader,
        paramTypeLoader: ParamTypeLoader
) {

    private val interpreter = Interpreter(scriptLoader, paramTypeLoader)

    fun <A : Appendable> decompile(id: Int, appendable: A): A {
        write(appendable, interpreter.interpret(id))
        return appendable
    }
}