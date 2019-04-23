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

    private val cache = HashMap<Int, Function>()

    fun interpret(id: Int): Function {
        return cache[id] ?: interpret0(id, checkNotNull(scriptLoader.load(id)))
    }

    private fun interpret0(id: Int, script: Script): Function {
        return makeFunction(id, script, interpretInstructions(id, script))
    }

    private fun interpretInstructions(id: Int, script: Script): Array<Instruction> {
        val state = State(this, id, script)
        return Array(script.opcodes.size) {
            val insn = Op.translate(state)
            state.pc++
            if (insn !is Instruction.Assignment) {
                check(state.stack.isEmpty())
            }
            insn
        }
    }

    private fun makeFunction(id: Int, script: Script, instructions: Array<Instruction>): Function {
        val returnInstruction = instructions.last { it is Instruction.Return } as Instruction.Return
        val args = ArrayList<Element.Variable.Local>()
        repeat(script.intArgumentCount) { args.add(Element.Variable.Local(it, Type.INT)) }
        repeat(script.stringArgumentCount) { args.add(Element.Variable.Local(it, Type.STRING)) }
        val returnTypes = returnInstruction.expression.list<Expression>().flatMap { it.types }
        val f = Function(id, args, addLabels(instructions), returnTypes)
        Phase.DEFAULT.transform(f)
        cache[id] = f
        return f
    }

    private fun addLabels(instructions: Array<Instruction>): Chain<Instruction> {
        val chain = HashChain<Instruction>()
        val labels = HashSet<Int>()
        instructions.forEach { insn ->
            when (insn) {
                is Instruction.Branch -> labels.add(insn.pass.id)
                is Instruction.Goto -> labels.add(insn.label.id)
                is Instruction.Switch -> insn.map.values.mapTo(labels) { it.id }
            }
        }
        instructions.forEachIndexed { index, insn ->
            if (index in labels) {
                chain.addLast(Instruction.Label(index))
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

        val stack: ListStack<StackValue> = ListStack()

        private var stackIdCounter: Int = 0

        val arrayTypes: Array<Type?> = arrayOfNulls(5)

        val opcode: Int get() = script.opcodes[pc].toUnsignedInt()

        val intOperand: Int get() = script.intOperands[pc]

        val stringOperand: String? get() = script.stringOperands[pc]

        fun operand(type: Type): Element.Constant = Element.Constant(if (type == Type.STRING) stringOperand else intOperand, type)

        val switch: Map<Int, Int> get() = script.switches[intOperand]

        fun pop(type: Type): Element.Variable.Stack = stack.pop().toExpression(type)

        fun takeAll(): List<Element.Variable.Stack> = stack.takeAll().map { it.toExpression() }

        fun take(types: List<Type>): List<Element.Variable.Stack> {
            val ts = types.toMutableList()
            val v = ArrayList<Element.Variable.Stack>(types.size)
            while (ts.isNotEmpty()) {
                val idx = if (stack.peek().type == Type.STRING) {
                    ts.indexOfLast { it == Type.STRING }
                } else {
                    ts.indexOfLast { it != Type.STRING }
                }
                v.add(pop(ts.removeAt(idx)))
            }
            v.reverse()
            return v
        }

        fun push(type: Type, value: Any? = null): Element.Variable.Stack {
            val v = StackValue(value, type, ++stackIdCounter)
            stack.push(v)
            return v.toExpression()
        }
    }
}