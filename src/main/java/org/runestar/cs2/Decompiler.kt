package org.runestar.cs2

import org.runestar.cs2.bin.ScriptLoader
import org.runestar.cs2.cg.write
import org.runestar.cs2.dfa.Phase
import org.runestar.cs2.ir.Interpreter

data class Decompiler(val scriptLoader: ScriptLoader) {

    private val interpreter = Interpreter(scriptLoader)

    fun decompile(id: Int, appendable: Appendable): Appendable {
        write(appendable, interpreter.interpret(id))
        return appendable
    }
}