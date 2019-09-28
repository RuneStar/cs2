package org.runestar.cs2.ir

import org.runestar.cs2.Loader
import org.runestar.cs2.Primitive
import org.runestar.cs2.Script
import org.runestar.cs2.Type
import org.runestar.cs2.loadNotNull
import org.runestar.cs2.util.Chain
import org.runestar.cs2.util.HashChain
import org.runestar.cs2.util.ListStack
import org.runestar.cs2.util.toUnsignedInt

internal class Interpreter(
        val scripts: Loader<Script>,
        val paramTypes: Loader<Primitive>
) {

    fun interpret(id: Int): Function {
        val script = scripts.loadNotNull(id)
        return makeFunction(id, script, interpretInstructions(script))
    }

    private fun interpretInstructions(script: Script): Array<Instruction> {
        val state = State(scripts, paramTypes, script)
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
        val args = ArrayList<Element.Variable>(script.intArgumentCount + script.stringArgumentCount)
        repeat(script.intArgumentCount) { args.add(Element.Variable(VarSource.LOCALINT, it, Primitive.INT)) }
        repeat(script.stringArgumentCount) { args.add(Element.Variable(VarSource.LOCALSTRING, it, Primitive.STRING)) }
        return Function(id, args, addLabels(instructions), script.returnTypes)
    }

    private fun addLabels(instructions: Array<Instruction>): Chain<Instruction> {
        val chain = HashChain<Instruction>()
        val labels = HashSet<Int>()
        for (insn in instructions) {
            when (insn) {
                is Instruction.Branch -> labels.add(insn.pass.id)
                is Instruction.Goto -> labels.add(insn.label.id)
                is Instruction.Switch -> insn.cases.values.mapTo(labels) { it.id }
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
            val scripts: Loader<Script>,
            val paramTypes: Loader<Primitive>,
            private val script: Script
    ) {

        var pc: Int = 0

        val stack: ListStack<StackValue> = ListStack()

        private var stackIdCounter: Int = 0

        val opcode: Int get() = script.opcodes[pc].toUnsignedInt()

        val intOperand: Int get() = script.operands[pc] as Int

        val stringOperand: String get() = script.operands[pc] as String

        fun operand(type: Type): Element.Constant = Element.Constant(if (type == Primitive.STRING) stringOperand else intOperand, type)

        val switch: Map<Int, Int> get() = script.switches[intOperand]

        fun peekValue(): Any? = stack.peek().value

        fun popValue(): Any? = stack.pop().value

        fun pop(type: Type): Element.Variable = stack.pop().toExpression(type)

        fun popAll(): List<Element.Variable> = stack.popAll().map { it.toExpression() }

        fun pop(count: Int): List<Element.Variable> = stack.pop(count).map { it.toExpression() }

        fun pop(types: List<Type>): List<Element.Variable> = stack.pop(types.size).mapIndexed { i, x -> x.toExpression(types[i])}

        fun push(type: Type, value: Any? = null): Element.Variable {
            val v = StackValue(value, type, ++stackIdCounter)
            stack.push(v)
            return v.toExpression()
        }

        fun push(types: List<Type>): List<Element.Variable> = types.map { push(it) }
    }
}