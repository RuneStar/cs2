package org.runestar.cs2

import org.runestar.cs2.bin.Loader
import org.runestar.cs2.bin.Script
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

    fun <A : Appendable> decompile(id: Int, appendable: A): A {
        val func = interpreter.interpret(id)
        generator.write(appendable, func, reconstruct(func))
        return appendable
    }

    fun decompile(id: Int): String = decompile(id, StringBuilder()).toString()
}