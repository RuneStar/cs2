package org.runestar.cs2.ir

import org.runestar.cs2.Loader
import org.runestar.cs2.Primitive
import org.runestar.cs2.Script
import org.runestar.cs2.StackType
import org.runestar.cs2.Type
import org.runestar.cs2.Value
import org.runestar.cs2.loadNotNull
import org.runestar.cs2.util.Chain
import org.runestar.cs2.util.HashChain
import org.runestar.cs2.util.ListStack
import org.runestar.cs2.util.toUnsignedInt

class Interpreter(
        val scripts: Loader<Script>,
        val commands: Loader<Command>,
        val paramTypes: Loader<Primitive>
) {

    private val globalTypingFactory = TypingFactory()

    fun interpret(scriptId: Int): Function {
        val state = State(scripts, paramTypes, scriptId, scripts.loadNotNull(scriptId), globalTypingFactory.Local())
        return makeFunction(state, interpretInstructions(state))
    }

    private fun interpretInstructions(state: State): Array<Instruction> {
        return Array(state.script.opcodes.size) {
            val insn = translate(state)
            state.pc++
            if (insn !is Instruction.Assignment) {
                check(state.stack.isEmpty())
            }
            insn
        }
    }

    private fun translate(state: State) = commands.loadNotNull(state.opcode).translate(state)

    private fun makeFunction(state: State, instructions: Array<Instruction>): Function {
        val args = ArrayList<Element.Variable>(state.script.intArgumentCount + state.script.stringArgumentCount)
        repeat(state.script.intArgumentCount) {
            val v = state.variable(VarSource.LOCALINT, it)
            v.typing.stackType = StackType.INT
            v.typing.isParameter = true
            args.add(v)
        }
        repeat(state.script.stringArgumentCount) {
            val v = state.variable(VarSource.LOCALSTRING, it)
            v.typing.stackType = StackType.STRING
            v.typing.isParameter = true
            args.add(v)
        }
        val returnTypes = state.typingFactory.returned(state.scriptId, state.script.returnTypes.size)
        return Function(state.scriptId, args, addLabels(instructions), returnTypes)
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

    class State(
            val scripts: Loader<Script>,
            val paramTypes: Loader<Primitive>,
            val scriptId: Int,
            val script: Script,
            val typingFactory: TypingFactory.Local
    ) {

        var pc: Int = 0

        val stack: ListStack<Element.Variable> = ListStack(ArrayList())

        private var stackIdCounter: Int = 0

        val opcode: Int get() = script.opcodes[pc].toUnsignedInt()

        val operand: Value get() = script.operands[pc]

        val switch: Map<Int, Int> get() = script.switches[operand.int]

        private fun pop(stackType: StackType, type: Type.Stackable?): Element.Variable {
            if (type != null) check(stackType == type.stackType)
            val v = stack.pop()
            check(v.typing.stackType == stackType)
            v.typing.to(type)
            return v
        }

        fun pop(type: Type.Stackable): Element.Variable = pop(type.stackType, type)

        fun pop(stackType: StackType): Element.Variable = pop(stackType, null)

        fun popAll(): List<Element.Variable> = stack.popAll()

        fun pop(count: Int): List<Element.Variable> = stack.pop(count)

        fun pop(types: List<Type.Stackable>): List<Element.Variable> = stack.pop(types.size)
                .also { it.forEachIndexed { i, v -> v.typing.to(types[i]) } }

        private fun push(stackType: StackType, type: Type.Stackable?, value: Value?): Element.Variable {
            if (type != null) check(stackType == type.stackType)
            val typing = Typing.from(type)
            typing.stackType = stackType
            val v = Element.Variable(VarId(VarSource.STACK, stackIdCounter++), typing, value)
            stack.push(v)
            return v
        }

        fun push(stackType: StackType, value: Value? = null): Element.Variable = push(stackType, null, value)

        fun push(type: Type.Stackable, value: Value? = null): Element.Variable = push(type.stackType, type, value)

        fun push(types: List<Type.Stackable>): List<Element.Variable> = types.map { push(it) }

        @JvmName("push2")
        fun push(stackTypes: List<StackType>): List<Element.Variable> = stackTypes.map { push(it) }

        fun variable(varSource: VarSource, id: Int): Element.Variable {
            val varId = VarId(varSource, id)
            return Element.Variable(varId, typingFactory.variable(varId))
        }
    }
}