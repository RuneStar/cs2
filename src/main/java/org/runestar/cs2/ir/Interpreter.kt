package org.runestar.cs2.ir

import org.runestar.cs2.Loader
import org.runestar.cs2.Primitive
import org.runestar.cs2.Script
import org.runestar.cs2.StackType
import org.runestar.cs2.Type
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
            val vid = VarId(VarSource.LOCALINT, it)
            val t = state.typingFactory.variable(vid)
            t.stackType = StackType.INT
            t.isParameter = true
            args.add(Element.Variable(vid, t))
        }
        repeat(state.script.stringArgumentCount) {
            val vid = VarId(VarSource.LOCALSTRING, it)
            val t = state.typingFactory.variable(vid)
            t.stackType = StackType.STRING
            t.isParameter = true
            args.add(Element.Variable(vid, t))
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

        val stack: ListStack<StackValue> = ListStack(ArrayList())

        private var stackIdCounter: Int = 0

        val opcode: Int get() = script.opcodes[pc].toUnsignedInt()

        val intOperand: Int get() = script.operands[pc] as Int

        val stringOperand: String get() = script.operands[pc] as String

        fun operandValue(stackType: StackType): Any = when (stackType) {
            StackType.INT -> intOperand
            StackType.STRING -> stringOperand
        }

        fun operand(stackType: StackType) = Element.Constant(operandValue(stackType), Typing())

        fun operand(type: Type.Stackable) = Element.Constant(operandValue(type.stackType), Typing.to(type))

        val switch: Map<Int, Int> get() = script.switches[intOperand]

        fun peekValue(): Any? = stack.peek().value

        fun popValue(): Any? = stack.pop().value

        fun pop(stackType: StackType, type: Type.Stackable? = null): Element.Variable {
            if (type != null) check(stackType == type.stackType)
            val p = stack.pop()
            check(p.stackType == stackType)
            p.typing.to(type)
            return p.toExpression()
        }

        fun pop(type: Type.Stackable): Element.Variable = pop(type.stackType, type)

        fun popAll(): List<Element.Variable> = stack.popAll().map { it.toExpression() }

        fun pop(count: Int): List<Element.Variable> = stack.pop(count).map { it.toExpression() }

        fun pop(types: List<Type.Stackable>): List<Element.Variable> = stack.pop(types.size).mapIndexed { i, sv ->
            sv.typing.to(types[i])
            sv.toExpression()
        }

        fun push(stackType: StackType, type: Type.Stackable? = null, value: Any? = null): Element.Variable {
            if (type != null) check(stackType == type.stackType)
            val v = StackValue(stackIdCounter++, value, stackType, Typing.from(type))
            stack.push(v)
            return v.toExpression()
        }

        fun push(stackType: StackType, value: Any? = null): Element.Variable = push(stackType, null, value)

        fun push(type: Type.Stackable, value: Any? = null): Element.Variable = push(type.stackType, type, value)

        fun push(types: List<Type.Stackable>): List<Element.Variable> = types.map { push(it) }

        @JvmName("push2")
        fun push(stackTypes: List<StackType>): List<Element.Variable> = stackTypes.map { push(it) }

        fun variable(varSource: VarSource, id: Int): Element.Variable {
            val varId = VarId(varSource, id)
            return Element.Variable(varId, typingFactory.variable(varId))
        }
    }
}

class StackValue(val id: Int, val value: Any?, val stackType: StackType, val typing: Typing) {

    fun toExpression() = Element.Variable(VarId(VarSource.STACK, id), typing).also { typing.stackType = stackType }
}