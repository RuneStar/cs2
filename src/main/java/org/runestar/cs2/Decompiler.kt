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

    fun decompile(id: Int, appendable: StringBuilder): StringBuilder {
        val func = interpreter.interpret(id)
        generator.write(appendable, func, reconstruct(func))
        return appendable
    }

    fun decompile(id: Int): String = decompile(id, StringBuilder()).toString()
}