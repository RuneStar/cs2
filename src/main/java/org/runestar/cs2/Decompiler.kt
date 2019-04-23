package org.runestar.cs2

import org.runestar.cs2.cfa.reconstruct
import org.runestar.cs2.cg.Generator
import org.runestar.cs2.cg.StrictGenerator
import org.runestar.cs2.ir.Interpreter

class Decompiler(
        scriptLoader: Loader<Script>,
        paramTypeLoader: Loader<Type> = Loader.PARAM_TYPES,
        private val generator: Generator = StrictGenerator()
) {

    private val interpreter = Interpreter(scriptLoader, paramTypeLoader)

    fun decompile(id: Int, buf: StringBuilder): StringBuilder {
        val f = interpreter.interpret(id)
        generator.write(buf, f, reconstruct(f))
        return buf
    }

    fun decompile(id: Int): String = decompile(id, StringBuilder()).toString()
}