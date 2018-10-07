package org.runestar.cs2.ir

import org.runestar.cs2.Type
import org.runestar.cs2.bin.ParamTypeLoader
import org.runestar.cs2.bin.Script
import org.runestar.cs2.bin.ScriptLoader
import org.runestar.cs2.bin.toUnsignedInt
import org.runestar.cs2.dfa.Phase
import org.runestar.cs2.util.Chain
import org.runestar.cs2.util.HashChain
import org.runestar.cs2.util.ListStack

internal class Interpreter(
        val scriptLoader: ScriptLoader,
        val paramTypeLoader: ParamTypeLoader
) {

    private val cache = HashMap<Int, Func>()

    fun interpret(id: Int): Func {
        val cached = cache[id]
        if (cached != null) return cached
        val script = scriptLoader.load(id)
        val insns = arrayOfNulls<Insn?>(script.opcodes.size)
        interpret(insns, State(this, id, script))
        val returnInsn = (insns.last { it != null && it is Insn.Return } as Insn.Return).expr as Expr.Operation
        val args = ArrayList<Expr.Var>()
        repeat(script.intArgumentCount) { args.add(Expr.Var(it, Type.INT)) }
        repeat(script.stringArgumentCount) { args.add(Expr.Var(it, Type.STRING)) }
        val returns = returnInsn.arguments.flatMapTo(ArrayList()) { it.types }
        val func = Func(id, args, addLabels(insns), returns)
        Phase.DEFAULT.transform(func)
        cache[id] = func
        return func
    }

    private fun interpret(insns: Array<Insn?>, state: State) {
        if (insns[state.pc] != null) return
        val op = Op.of(state.script.opcodes[state.pc].toUnsignedInt())
        val insn = op.translate(state)
        insns[state.pc] = insn
        val successors = successorPcs(state.pc, insn)
        if (insn !is Insn.Assignment) {
            check(state.intStack.isEmpty())
            check(state.strStack.isEmpty())
        }
        for (s in successors) {
            state.pc = s
            interpret(insns, state)
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

    internal class State(
            val interpreter: Interpreter,
            val id: Int,
            val script: Script
    ) {

        var pc: Int = 0

        val intStack: ListStack<Val<Int?>> = ListStack(ArrayList())

        val strStack: ListStack<Val<String?>> = ListStack(ArrayList())

        private var stackVarCounter: Int = 0

        val arrayTypes: Array<Type?> = arrayOfNulls(5)

        val intOperand: Int get() = script.intOperands[pc]

        val strOperand: String? get() = script.stringOperands[pc]

        fun operand(type: Type): Expr.Cst {
            val o: Any? = when (type.topType == Type.STRING) {
                true -> strOperand
                false -> intOperand
            }
            return Expr.Cst(type, o)
        }

        val switch: Map<Int, Int> get() = script.switches[intOperand]

        fun pop(type: Type): Expr.Var = when (type.topType == Type.STRING) {
            false -> intStack.pop().toExpr(type)
            true -> strStack.pop().toExpr(type)
        }

        fun push(type: Type, cst: Any? = null): Expr.Var {
            return when (type.topType == Type.STRING) {
                false -> {
                    val v = Val(cst as Int?, type, ++stackVarCounter)
                    intStack.push(v)
                    v.toExpr()
                }
                true -> {
                    val v = Val(cst as String?, type, ++stackVarCounter)
                    strStack.push(v)
                    v.toExpr()
                }
            }
        }
    }
}