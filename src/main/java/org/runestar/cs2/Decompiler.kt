package org.runestar.cs2

import org.runestar.cs2.bin.NameLoader
import org.runestar.cs2.bin.ParamTypeLoader
import org.runestar.cs2.bin.ScriptLoader
import org.runestar.cs2.cg.Generator
import org.runestar.cs2.ir.Interpreter

class Decompiler(
        scriptLoader: ScriptLoader,
        paramTypeLoader: ParamTypeLoader = ParamTypeLoader.DEFAULT,
        fontNameLoader: NameLoader = NameLoader.FONTS,
        graphicNameLoader: NameLoader = NameLoader.GRAPHICS,
        scriptNameLoader: NameLoader = NameLoader.SCRIPTS,
        statNameLoader: NameLoader = NameLoader.STATS
) {

    private val interpreter = Interpreter(scriptLoader, paramTypeLoader)

    private val generator = Generator(fontNameLoader, graphicNameLoader, scriptNameLoader, statNameLoader)

    fun <A : Appendable> decompile(id: Int, appendable: A): A {
        generator.write(appendable, interpreter.interpret(id))
        return appendable
    }

    fun decompile(id: Int): String = decompile(id, StringBuilder()).toString()
}