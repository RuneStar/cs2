package org.runestar.cs2.ir

import org.runestar.cs2.bin.Script
import org.runestar.cs2.bin.StackType
import org.runestar.cs2.bin.Type
import org.runestar.cs2.bin.Value
import org.runestar.cs2.bin.int
import org.runestar.cs2.util.Chain
import org.runestar.cs2.util.HashChain
import org.runestar.cs2.util.ListStack
import org.runestar.cs2.util.Loader
import org.runestar.cs2.util.counter
import org.runestar.cs2.util.loadNotNull
import org.runestar.cs2.util.toUnsignedInt

fun interpret(
        scripts: Loader.Keyed<Script>,
        commands: Loader<Command>,
        paramTypes: Loader<Type>
) : FunctionSet {
    return Interpreter(scripts, commands, paramTypes).interpret()
}

private class Interpreter(
        private val scripts: Loader.Keyed<Script>,
        private val commands: Loader<Command>,
        private val paramTypes: Loader<Type>
) {

    private val typings = Typings()

    private val callGraph = CallGraph()

    fun interpret() = FunctionSet(scripts.ids.associateWith { interpret(it) }, typings, callGraph)

    private fun interpret(scriptId: Int): Function {
        val state = state(scriptId)
        val instructions = addLabels(interpretInstructions(state))
        val args = ArrayList<Variable.Local>(state.script.intArgumentCount + state.script.stringArgumentCount)
        repeat(state.script.intArgumentCount) {
            args.add(Variable.int(state.scriptId, it))
        }
        repeat(state.script.stringArgumentCount) {
            args.add(Variable.string(state.scriptId, it))
        }
        return Function(scriptId, args, instructions, state.script.returnTypes)
    }

    private fun state(scriptId: Int) = InterpreterState(scripts, paramTypes, scriptId, scripts.loadNotNull(scriptId), typings, callGraph)

    private fun interpretInstructions(state: InterpreterState): Array<Instruction> {
        return Array(state.script.opcodes.size) {
            val insn = translate(state)
            state.pc++
            if (insn !is Instruction.Assignment) {
                 check(state.stack.isEmpty())
            }
            insn
        }
    }

    private fun translate(state: InterpreterState) = commands.loadNotNull(state.opcode).translate(state)

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
}

class InterpreterState(
        val scripts: Loader<Script>,
        val paramTypes: Loader<Type>,
        val scriptId: Int,
        val script: Script,
        val typings: Typings,
        val callGraph: CallGraph,
) {

    var pc: Int = 0

    internal val stack: ListStack<Element.Access> = ListStack(ArrayList())

    private val stackCounter = counter()

    val opcode: Int get() = script.opcodes[pc].toUnsignedInt()

    val operand: Value get() = script.operands[pc]

    val switch: Map<Int, Int> get() = script.switches[operand.int]

    fun push(stackType: StackType, value: Value? = null): Element.Access {
        if (value != null) require(stackType == value.stackType)
        val id = stackCounter()
        val variable = when (stackType) {
            StackType.INT -> Variable.stackint(scriptId, id)
            StackType.STRING -> Variable.stackstring(scriptId, id)
        }
        return Element.Access(variable, value).also { stack.push(it) }
    }

    fun push(stackTypes: List<StackType>): List<Element.Access> = stackTypes.map { push(it) }

    fun pop(stackType: StackType): Element.Access = stack.pop().also { check(it.stackType == stackType) }

    fun pop(count: Int): List<Element.Access> = stack.pop(count)

    fun popAll(): List<Element.Access> = stack.popAll()

    fun peekValue(): Value? = stack.peek().value

    fun popValue(): Value = checkNotNull(stack.pop().value)
}