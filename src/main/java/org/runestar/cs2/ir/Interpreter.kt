package org.runestar.cs2.ir

import org.runestar.cs2.TopType
import org.runestar.cs2.Type
import org.runestar.cs2.bin.Script
import org.runestar.cs2.bin.ScriptLoader
import org.runestar.cs2.util.*

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
        val args = ArrayList<Expr.Var>()
        repeat(script.intArgumentCount) { args.add(Expr.Var.li(it)) }
        repeat(script.stringArgumentCount) { args.add(Expr.Var.ls(it)) }
        val returns = returnInsn.arguments.flatMapTo(ArrayList()) { it.types }
        val func = Func(id, args, addLabels(insns), returns)
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
            val intStack: ListStack<Expr.Cst> = ListStack(ArrayList()),
            val strStack: ListStack<Expr.Cst> = ListStack(ArrayList())
    ) {

        fun pop(type: Type): Expr.Var = when (type.topType) {
            TopType.INT -> intStack.pop().let { Expr.Var("${it.type.literal}${intStack.size}", it.type) }
            TopType.STRING -> strStack.pop().let { Expr.Var("${it.type.literal}${strStack.size}", it.type) }
        }

        fun peekCst(type: Type): Expr.Cst = when (type.topType) {
            TopType.INT -> intStack.peek()
            TopType.STRING -> strStack.peek()
        }

        fun push(cst: Expr.Cst): Expr.Var {
            return when (cst.type.topType) {
                TopType.INT -> {
                    intStack.push(cst)
                    Expr.Var("${cst.type.literal}${intStack.size - 1}", cst.type)
                }
                TopType.STRING -> {
                    strStack.push(cst)
                    Expr.Var("${cst.type.literal}${strStack.size - 1}", cst.type)
                }
            }
        }

        fun push(type: Type): Expr.Var = push(Expr.Cst(type, null))

        fun copy(newPc: Int): State = State(script, loader, newPc, ListStack(intStack.delegate), ListStack(strStack.delegate))
    }
}