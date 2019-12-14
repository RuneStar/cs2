package org.runestar.cs2

import org.runestar.cs2.cfa.reconstruct
import org.runestar.cs2.cg.Generator
import org.runestar.cs2.dfa.Phase
import org.runestar.cs2.ir.Command
import org.runestar.cs2.ir.Interpreter

fun decompile(
        scripts: Loader.Keyed<Script>,
        generator: Generator,
        commands: Loader<Command> = Command.LOADER,
        paramTypes: Loader<Primitive> = Loader.PARAM_TYPES
) {
    val interpreter = Interpreter(scripts, commands, paramTypes)
    val fs = scripts.ids.associateWith { interpreter.interpret(it) }
    Phase.DEFAULT.transform(fs)
    fs.values.forEach { generator.write(it, reconstruct(it)) }
}