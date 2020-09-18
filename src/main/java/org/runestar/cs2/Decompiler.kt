package org.runestar.cs2

import org.runestar.cs2.bin.Script
import org.runestar.cs2.bin.Type
import org.runestar.cs2.cfa.reconstruct
import org.runestar.cs2.cg.Generator
import org.runestar.cs2.dfa.Phase
import org.runestar.cs2.ir.Command
import org.runestar.cs2.ir.interpret
import org.runestar.cs2.util.Loader

fun decompile(
        scripts: Loader.Keyed<Script>,
        generator: Generator,
        commands: Loader<Command> = Command.LOADER,
        paramTypes: Loader<Type> = PARAM_TYPES,
) {
    val fs = interpret(scripts, commands, paramTypes)
    Phase.DEFAULT.transform(fs)
    fs.functions.values.forEach { generator.write(it, fs, reconstruct(it)) }
}