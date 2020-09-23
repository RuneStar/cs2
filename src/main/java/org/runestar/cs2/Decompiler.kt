package org.runestar.cs2

import org.runestar.cs2.bin.Script
import org.runestar.cs2.cfa.reconstruct
import org.runestar.cs2.cg.Generator
import org.runestar.cs2.dfa.Phase
import org.runestar.cs2.ir.Command
import org.runestar.cs2.ir.interpret
import org.runestar.cs2.util.Loader

fun decompile(
        scripts: Loader.Keyed<Script>,
        generator: Generator,
) {
    val fs = interpret(scripts, Command.LOADER, PARAM_TYPES)
    Phase.DEFAULT.transform(fs)
    fs.functions.values.forEach { generator.write(it, fs, reconstruct(it)) }
}