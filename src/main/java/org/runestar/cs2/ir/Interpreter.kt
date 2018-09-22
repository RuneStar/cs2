package org.runestar.cs2.ir

import org.runestar.cs2.TopType
import org.runestar.cs2.Type
import org.runestar.cs2.bin.Script
import org.runestar.cs2.bin.ScriptLoader
import org.runestar.cs2.dfa.Phase
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
        interpret(insns, State(id, script, loader, 0))
        val returnInsn = (insns.last { it != null && it is Insn.Return } as Insn.Return).expr as Expr.Operation
        val args = ArrayList<Expr.Var>()
        repeat(script.intArgumentCount) { args.add(Expr.Var.l(it, Type.INT)) }
        repeat(script.stringArgumentCount) { args.add(Expr.Var.l(it, Type.STRING)) }
        val returns = returnInsn.arguments.flatMapTo(ArrayList()) { it.types }
        val func = Func(id, args, addLabels(insns), returns)
        Phase.DEFAULT.transform(func)
        cache[id] = func
        return func
    }

    private fun interpret(insns: Array<Insn?>, state: State) {
        if (insns[state.pc] != null) return
        val op = Op.of(state.script.opcodes[state.pc])
        val insn = op.translate(state)
        insns[state.pc] = insn
        val successors = successorPcs(state.pc, insn)
        if (successors.size == 1) {
            state.pc = successors.single()
            interpret(insns, state)
        } else if (successors.size > 1) {
            val copy = state.copy()
            for (s in successors) {
                interpret(insns, copy.copy(s))
            }
        }
    }

    private fun successorPcs(pc: Int, insn: Insn): Collection<Int> {
        return when (insn) {
            is Insn.Assignment -> listOf(pc + 1)
            is Insn.Goto -> listOf(insn.label.id)
            is Insn.Branch -> listOf(pc + 1, insn.pass.id)
            is Insn.Return -> emptyList()
            is Insn.Switch -> {
                val list = ArrayList<Int>(1 + insn.map.size)
                list.add(pc + 1)
                insn.map.values.mapTo(list) { it.id }
            }
            else -> error(insn)
        }
    }

    private fun addLabels(insns: Array<Insn?>): Chain<Insn> {
        val chain = HashChain<Insn>()
        val labels = HashSet<Int>()
        insns.forEach { insn ->
            when (insn) {
                is Insn.Branch -> labels.add(insn.pass.id)
                is Insn.Goto -> labels.add(insn.label.id)
                is Insn.Switch -> insn.map.values.mapTo(labels) { it.id }
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

    class State(
            val id: Int,
            val script: Script,
            val loader: ScriptLoader,
            var pc: Int,
            val intStack: ListStack<Expr.Cst> = ListStack(ArrayList()),
            val strStack: ListStack<Expr.Cst> = ListStack(ArrayList())
    ) {

        val intOperand: Int get() = script.intOperands[pc]

        val strOperand: String? get() = script.stringOperands[pc]

        fun operand(type: Type): Expr.Cst {
            val o: Any? = when (type.topType) {
                TopType.STRING -> strOperand
                TopType.INT -> intOperand
            }
            return Expr.Cst(type, o)
        }

        val switch: Map<Int, Int> get() = script.switches[intOperand]

        fun pop(type: Type): Expr.Var = when (type.topType) {
            TopType.INT -> {
                val expr = intStack.pop()
                val t = Type.bottom(expr.type, type)
                Expr.Var("#${Type.INT.desc}${intStack.size}", t)
            }
            TopType.STRING -> {
                strStack.pop()
                Expr.Var("#${Type.STRING.desc}${strStack.size}", type)
            }
        }

        fun peekCst(type: Type): Expr.Cst = when (type.topType) {
            TopType.INT -> intStack.peek()
            TopType.STRING -> strStack.peek()
        }

        fun push(cst: Expr.Cst): Expr.Var {
            return when (cst.type.topType) {
                TopType.INT -> {
                    intStack.push(cst)
                    Expr.Var("#${Type.INT.desc}${intStack.size - 1}", cst.type)
                }
                TopType.STRING -> {
                    strStack.push(cst)
                    Expr.Var("#${Type.STRING.desc}${strStack.size - 1}", cst.type)
                }
            }
        }

        fun push(type: Type): Expr.Var = push(Expr.Cst(type, null))

        fun copy(newPc: Int = pc) = State(id, script, loader, newPc, ListStack(ArrayList(intStack.delegate)), ListStack(ArrayList(strStack.delegate)))
    }
}