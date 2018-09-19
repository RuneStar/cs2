package org.runestar.cs2

import org.runestar.cs2.bin.ScriptLoader
import org.runestar.cs2.cg.write
import org.runestar.cs2.dfa.Phase
import org.runestar.cs2.ir.Interpreter

data class Decompiler(val scriptLoader: ScriptLoader) {

    private val interpreter = Interpreter(scriptLoader)

    fun decompile(id: Int, appendable: Appendable): Appendable {
        val func = interpreter.interpret(id)
        Phase.DEFAULT.transform(func)
        write(appendable, func)
        return appendable
    }
}