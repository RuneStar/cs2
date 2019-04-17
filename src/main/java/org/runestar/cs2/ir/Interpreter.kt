package org.runestar.cs2.ir

import org.runestar.cs2.Loader
import org.runestar.cs2.Script
import org.runestar.cs2.Type
import org.runestar.cs2.dfa.Phase
import org.runestar.cs2.util.Chain
import org.runestar.cs2.util.HashChain
import org.runestar.cs2.util.ListStack
import org.runestar.cs2.util.toUnsignedInt

internal class Interpreter(
        val scriptLoader: Loader<Script>,
        val paramTypeLoader: Loader<Type>
) {

    private val cache = HashMap<Int, Func>()

    fun interpret(id: Int): Func {
        return cache[id] ?: interpret0(id, checkNotNull(scriptLoader.load(id)))
    }

    private fun interpret0(id: Int, script: Script): Func {
        return makeFunc(id, script, interpretInsns(id, script))
    }

    private fun interpretInsns(id: Int, script: Script): Array<Insn> {
        val state = State(this, id, script)
        return Array(script.opcodes.size) {
            val insn = Op.translate(state)
            state.pc++
            if (insn !is Insn.Assignment) {
                check(state.stack.isEmpty())
            }
            insn
        }
    }

    private fun makeFunc(id: Int, script: Script, insns: Array<Insn>): Func {
        val returnInsn = (insns.last { it is Insn.Return } as Insn.Return).expr as Expr.Operation
        val args = ArrayList<Expr.Var>()
        repeat(script.intArgumentCount) { args.add(Expr.Var(it, Type.INT)) }
        repeat(script.stringArgumentCount) { args.add(Expr.Var(it, Type.STRING)) }
        val returns = returnInsn.arguments.flatMapTo(ArrayList()) { it.types }
        val func = Func(id, args, addLabels(insns), returns)
        Phase.DEFAULT.transform(func)
        cache[id] = func
        return func
    }

    private fun addLabels(insns: Array<Insn>): Chain<Insn> {
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
            chain.addLast(insn)
        }
        return chain
    }

    internal class State(
            val interpreter: Interpreter,
            val id: Int,
            val script: Script
    ) {

        var pc: Int = 0

        val stack: ListStack<Val> = ListStack()

        private var stackVarCounter: Int = 0

        val arrayTypes: Array<Type?> = arrayOfNulls(5)

        val opcode: Int get() = script.opcodes[pc].toUnsignedInt()

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

        fun pop(type: Type): Expr.Var = stack.pop().toExpr(type)

        fun popAll(): List<Expr.Var> = stack.popAll().map { it.toExpr() }

        fun pop(ints: List<Type>, strings: List<Type>): List<Expr.Var> {
            val i = ListStack(ArrayList(ints))
            val s = ListStack(ArrayList(strings))
            val v = ArrayList<Expr.Var>()
            while (!i.isEmpty() || !s.isEmpty()) {
                if (stack.peek().type == Type.STRING) {
                    v.add(pop(s.pop()))
                } else {
                    v.add(pop(i.pop()))
                }
            }
            return v
        }

        fun push(type: Type, cst: Any? = null): Expr.Var {
            val v = Val(cst, type, ++stackVarCounter)
            stack.push(v)
            return v.toExpr()
        }
    }
}