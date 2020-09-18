package org.runestar.cs2.dfa

import org.runestar.cs2.bin.*
import org.runestar.cs2.bin.StackType
import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.FunctionSet
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.Variable
import org.runestar.cs2.ir.asList
import org.runestar.cs2.ir.assign

object FindArrayArgs : Phase.Individual() {

    override fun transform(f: Function, fs: FunctionSet) {
        if (f.arguments.none { it.stackType == StackType.INT }) return
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val e = insn.expression
            if (e !is Expression.Operation) continue
            val args = e.arguments.asList
            when (e.opcode) {
                DEFINE_ARRAY -> {
                    if ((args[0] as Element.Access).variable.id == 0) return
                }
                PUSH_ARRAY_INT -> {
                    if ((args[0] as Element.Access).variable.id == 0) {

                        val oldVar = f.arguments[0]
                        val newVar = Variable.array(f.id, 0)
                        val newVars = f.arguments.toMutableList()
                        newVars[0] = newVar
                        f.arguments = newVars

                        val t = fs.typings.of(newVar)
                        val ts = fs.typings.args(f.id, f.arguments.map { it.stackType }).toMutableList()
                        ts[0] = t
                        fs.typings.remove(oldVar)
                        fs.typings.args[f.id] = ts
                        transformCalls(f.id, fs)
                        return
                    }
                }
            }
        }
    }

    private fun transformCalls(scriptId: Int, fs: FunctionSet) {
        for (f in fs.functions.values) {
            for (insn in f.instructions) {
                if (insn !is Instruction.Assignment) continue
                val e = insn.expression
                if (e !is Expression.Proc) continue
                if (e.scriptId != scriptId) continue

                // todo
                val oldArgs = e.arguments.asList
                val oldArg = oldArgs[0] as Element.Access
                val newArgs = oldArgs.toMutableList()
                val newArg = Element.Pointer(Variable.array(f.id, 0))
                newArgs[0] = newArg
                e.arguments = Expression(newArgs)
                if (f.id != scriptId) {
                    assign(fs.typings.of(newArg), fs.typings.args.getValue(scriptId)[0])
                }
            }
        }
    }
}