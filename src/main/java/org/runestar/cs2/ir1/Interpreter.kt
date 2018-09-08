package org.runestar.cs2.ir1

import org.runestar.cs2.raw.Script
import org.runestar.cs2.raw.ScriptLoader

class Interpreter(
        val loader: ScriptLoader
) {

    fun interpret(id: Int): Func {
        val script = checkNotNull(loader.load(id))
        val insns = arrayOfNulls<Insn?>(script.opcodes.size)
        interpret(insns, State(script, loader, 0))
        val insnList = insns.filterNotNull()
        val invokedReturn = insnList.last() as Insn.Return
        val returnedInts = invokedReturn.args.count { it is Arg.Var.Num }
        val returnedStrs = invokedReturn.args.count { it is Arg.Var.Str }
        return Func(script.intArgumentCount, script.stringArgumentCount, insnList, returnedInts, returnedStrs)
    }

    private fun interpret(insns: Array<Insn?>, state: State) {
        if (insns[state.pc] != null) return
        val opcode = state.script.opcodes[state.pc]
        val op = Op.of(opcode)
        val insn = op.translate(state)
//        println("${state.pc}\t$insn\n\t${state.intStack} ${state.strStack}")
        insns[state.pc] = insn
        val successors = insn.successors.iterator()
        if (successors.hasNext()) {
            state.pc = successors.nextInt()
            interpret(insns, state)
        }
        while (successors.hasNext()) {
            interpret(insns, state.copy(successors.nextInt()))
        }
    }

    class State(
            val script: Script,
            val loader: ScriptLoader,
            var pc: Int,
            val intStack: SparseStack<Int> = SparseStack(0),
            val strStack: SparseStack<String?> = SparseStack(null)
    ) {

        fun copy(newPc: Int): State = State(script, loader, newPc, intStack.copy(), strStack.copy())
    }
}