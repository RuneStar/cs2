package org.runestar.cs2

import org.runestar.cs2.cfa.reconstruct
import org.runestar.cs2.cg.Generator
import org.runestar.cs2.dfa.Phase
import org.runestar.cs2.ir.Interpreter

fun decompile(
        scripts: Loader.Keyed<Script>,
        generator: Generator,
        paramTypes: Loader<Type> = Loader.PARAM_TYPES
) {
    val interpreter = Interpreter(scripts, paramTypes)
    val fs = scripts.keys.associate { it to interpreter.interpret(it) }
    Phase.DEFAULT.transform(fs)
    fs.values.forEach { generator.write(it, reconstruct(it)) }
}