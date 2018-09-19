package org.runestar.cs2.ir

import org.runestar.cs2.TopType
import org.runestar.cs2.bin.Script
import org.runestar.cs2.bin.ScriptLoader
import org.runestar.cs2.util.Chain
import org.runestar.cs2.util.HashChain
import org.runestar.cs2.util.SparseStack

class Interpreter(
        val loader: ScriptLoader
) {

    private val cache = HashMap<Int, Func>()

    fun interpret(id: Int): Func {
        val cached = cache[id]
        if (cached != null) return cached
        val script = checkNotNull(loader.load(id))
        val insns = arrayOfNulls<Insn?>(script.opcodes.size)
        interpret(insns, State(script, loader, 0))
        val returnInsn = (insns.last { it != null && it is Insn.Return } as Insn.Return).expr as Expr.Operation
        val returnedInts = returnInsn.arguments.count { it is Expr.Var && it.type == TopType.INT }
        val returnedStrs = returnInsn.arguments.count { it is Expr.Var && it.type == TopType.STRING }
        val func = Func(id, script.intArgumentCount, script.stringArgumentCount, addLabels(insns), returnedInts, returnedStrs)
        cache[id] = func
        return func
    }

    private fun addLabels(insns: Array<Insn?>): Chain<Insn> {
        val chain = HashChain<Insn>()
        val labels = HashSet<Int>()
        insns.forEachIndexed { i, insn ->
            when (insn) {
                is Insn.Branch -> {
                    labels.add(insn.pass.name.toInt())
                }
                is Insn.Goto -> {
                    labels.add(insn.label.name.toInt())
                }
                is Insn.Switch -> {
                    insn.map.values.forEach { labels.add(it.name.toInt()) }
                }
            }
        }
        insns.forEachIndexed { index, insn ->
            if (index in labels) {
                chain.addLast(Insn.Label(index))
            }
            if (insn != null) {
                chain.addLast(insn)
            }
        }
        return chain
    }

    private fun interpret(insns: Array<Insn?>, state: State) {
        if (insns[state.pc] != null) return
        val opcode = state.script.opcodes[state.pc]
        val op = Op.of(opcode)
        val insn = op.translate(state)
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