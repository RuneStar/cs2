package org.runestar.cs2.ir

import org.runestar.cs2.TopType
import org.runestar.cs2.bin.Script
import org.runestar.cs2.bin.ScriptLoader

class Interpreter(
        val loader: ScriptLoader
) {

    fun interpret(id: Int): Func {
        val script = checkNotNull(loader.load(id))
        val insns = arrayOfNulls<Insn?>(script.opcodes.size)
        interpret(insns, State(script, loader, 0))
        val insnList = insns.filterNotNull()
        val invokedReturn = (insnList.last() as Insn.Return).expr as Expr.Operation
        val returnedInts = invokedReturn.arguments.count { it is Expr.Var && it.type == TopType.INT }
        val returnedStrs = invokedReturn.arguments.count { it is Expr.Var && it.type == TopType.STRING }
        val labels = HashSet<Insn.Label>()
        insnList.forEachIndexed { i, insn ->
            when (insn) {
                is Insn.Branch -> {
                    labels.add(insn.pass)
                }
                is Insn.Goto -> {
                    labels.add(insn.label)
                }
                is Insn.Switch -> {
                    labels.addAll(insn.map.values)
                }
            }
        }
        val labelIndices = labels.map { it.name.toInt() }
        val insnsLabeled = ArrayList<Insn>()
        insnList.forEachIndexed { index, insn ->
            if (index in labelIndices) {
                insnsLabeled.add(Insn.Label(index))
            }
            insnsLabeled.add(insn)
        }
        return Func(script.intArgumentCount, script.stringArgumentCount, insnsLabeled, returnedInts, returnedStrs)
    }

    private fun interpret(insns: Array<Insn?>, state: State) {
        if (insns[state.pc] != null) return
        val opcode = state.script.opcodes[state.pc]
        val op = Op.of(opcode)
        val insn = op.translate(state)
//        println("${state.pc}\t$insn\n\t${state.intStack} ${state.strStack}")
        insns[state.pc] = insn
        val successors = ArrayList<Int>()
        when (insn) {
            is Insn.Assignment -> successors.add(state.pc + 1)
            is Insn.Goto -> successors.add(insn.label.name.toInt())
            is Insn.Branch -> {
                successors.add(state.pc + 1)
                successors.add(insn.pass.name.toInt())
            }
            is Insn.Return -> return
            is Insn.Switch -> {
                successors.add(state.pc + 1)
                successors.addAll(insn.map.values.map { it.name.toInt() })
            }
        }
        val itr = successors.iterator()
        if (itr.hasNext()) {
            state.pc = itr.next()
            interpret(insns, state)
        }
        while (itr.hasNext()) {
            interpret(insns, state.copy(itr.next()))
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