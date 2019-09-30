package org.runestar.cs2.dfa

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Primitive
import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.Typing
import org.runestar.cs2.ir.VarId
import org.runestar.cs2.ir.VarSource
import org.runestar.cs2.ir.list
import org.runestar.cs2.util.removeFirst

class InferTypes(val fs: Map<Int, Function>) {

    companion object : Phase {
        override fun transform(fs: Map<Int, Function>) = InferTypes(fs).transform()
    }

    private val globalVarUsages = findGlobalVars()

    private val invokes = findInvokes()

    private val working = LinkedHashSet(fs.values)

    private fun transform() {
        reorderArgs()
        while (working.isNotEmpty()) propagate(working.removeFirst())
    }

    private fun propagate(f: Function) {
        while (
                findNamedObj(f) ||
                propagateAssignmentsTo(f) ||
                propagateAssignmentsFrom(f) ||
                propagateInvokes(f) ||
                propagateArrayGet(f) ||
                propagateArraySet(f) ||
                propagateReturns(f) ||
                propagateComparisons(f)
        );
    }

    private fun findGlobalVars(): Map<VarId, Set<Function>> {
        val vars = HashMap<VarId, MutableSet<Function>>()
        for (f in fs.values) {
            fun add(e: Expression) {
                if (e !is Element.Variable) return
                if (!e.varId.source.global) return
                vars.getOrPut(e.varId) { HashSet() }.add(f)
            }
            for (insn in f.instructions) {
                if (insn !is Instruction.Assignment) continue
                add(insn.definitions)
                add(insn.expression)
            }
        }
        return vars
    }

    private fun findInvokes(): Map<Int, Set<Function>> {
        val invokes = HashMap<Int, MutableSet<Function>>()
        for (f in fs.values) {
            for (insn in f.instructions) {
                if (insn !is Instruction.Assignment) continue
                val operation = insn.expression as? Expression.Operation.Scripted ?: continue
                invokes.getOrPut(operation.scriptId) { HashSet() }.add(f)
            }
        }
        return invokes
    }

    private fun reorderArgs() {
        for (f in fs.values) {
            for (insn in f.instructions) {
                if (insn !is Instruction.Assignment) continue
                val e = insn.expression
                if (e !is Expression.Operation.Scripted) continue
                e as Expression.Operation
                val s = fs[e.scriptId] ?: continue
                if (s.arguments.isEmpty()) continue
                val arguments = s.arguments.toMutableList()
                val newArguments = ArrayList<Element.Variable>()
                if (arguments[0].varId.source == VarSource.ARRAY) {
                    val first = e.scriptArguments.list<Expression>().first() as Element
                    first.typing.stackType = null
                }
                for (t in e.scriptArguments.typings) {
                    newArguments.add(arguments.removeAt(arguments.indexOfFirst { it.typing.stackType == t.stackType }))
                }
                s.arguments = newArguments
            }
        }
    }

    private fun findNamedObj(f: Function): Boolean {
        var b = false
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val e = insn.expression
            if (e !is Element.Constant) continue
            if (e.typing.type == Primitive.OBJ) {
                check(e.typing.to(Primitive.NAMEDOBJ))
                b = true
            }
        }
        return b
    }

    private fun propagateAssignmentsTo(f: Function): Boolean {
        var b = false
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val def = insn.definitions
            val left = def.typings
            val right = insn.expression.typings
            for (i in left.indices) {
                val c = left[i].from(right[i].type)
                b = b or c
                if (c && def is Element.Variable && def.varId.source.global) {
                    globalVarUsages[def.varId]?.let { working.addAll(it) }
                } else if (c && left[i].isParameter) {
                    invokes[f.id]?.let { working.addAll(it) }
                }
            }
        }
        return b
    }

    private fun propagateAssignmentsFrom(f: Function): Boolean {
        var b = false
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val expr = insn.expression
            val left = insn.definitions.typings
            val right = expr.typings
            for (i in left.indices) {
                val c = right[i].to(left[i].type)
                b = b or c
                if (c && expr is Element.Variable && expr.varId.source.global) {
                    globalVarUsages[expr.varId]?.let { working.addAll(it) }
                } else if (c && expr is Expression.Operation.Invoke) {
                    fs[expr.scriptId]?.let { working.add(it) }
                    invokes[expr.scriptId]?.let { working.addAll(it) }
                } else if (c && right[i].isParameter) {
                    invokes[f.id]?.let { working.addAll(it) }
                }
            }
        }
        return b
    }

    private fun propagateComparisons(f: Function): Boolean {
        var b = false
        for (insn in f.instructions) {
            if (insn !is Instruction.Branch) continue
            val operation = insn.expression as Expression.Operation
            val args = operation.arguments.list<Element.Variable>()
            val left = args[0]
            val right = args[1]
            b = b or Typing.compare(left.typing, right.typing)
        }
        return b
    }

    private fun propagateArrayGet(f: Function): Boolean {
        var b = false
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val operation = insn.expression as? Expression.Operation ?: continue
            if (operation.id == Opcodes.PUSH_ARRAY_INT) {
                val args = operation.arguments.list<Element.Variable>()
                val array = args[0]
                val elem = insn.definitions as Element.Variable
                b = b or Typing.arrayGet(array.typing, elem.typing)
            }
        }
        return b
    }

    private fun propagateArraySet(f: Function): Boolean {
        var b = false
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val operation = insn.expression as? Expression.Operation ?: continue
            if (operation.id == Opcodes.POP_ARRAY_INT) {
                val args = operation.arguments.list<Element>()
                val array = args[0]
                val elem = args[2]
                b = b or Typing.arraySet(array.typing, elem.typing)
            }
        }
        return b
    }

    private fun propagateReturns(f: Function): Boolean {
        val returns = ArrayList<List<Typing>>()
        for (insn in f.instructions) {
            if (insn !is Instruction.Return) continue
            returns.add(insn.expression.typings)
        }
        val b = Typing.returns(returns, f.returnTypes)
        if (b) {
            invokes[f.id]?.let { working.addAll(it) }
        }
        return b
    }

    private fun propagateInvokes(f: Function): Boolean {
        var b = false
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val e = insn.expression as? Expression.Operation.Scripted ?: continue
            val s = fs[e.scriptId] ?: continue
            val t0 = e.scriptArguments.typings
            val t1 = s.arguments.map { it.typing }
            check(t0.size == t1.size) { "$t0 $t1 $insn ${s.arguments}" }
            for (i in t0.indices) {
                b = b or t0[i].to(t1[i].type)
                if (t1[i].from(t0[i].type)) {
                    working.add(s)
                    invokes[e.scriptId]?.let { working.addAll(it) }
                }
            }
        }
        return b
    }
}