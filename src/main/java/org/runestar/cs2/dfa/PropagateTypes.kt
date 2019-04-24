package org.runestar.cs2.dfa

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type
import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.list

internal object PropagateTypes : Phase {

    override fun transform(f: Function) {
        while (
                propagateAssignments(f) or
                propagateVars(f) or
                propagateComparisons(f) or
                propagateReturns(f)
        ) { }
    }

    private fun propagateAssignments(f: Function): Boolean {
        var changed = false
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val oldLeft = insn.definitions.types
            val oldRight = insn.expression.types
            if (oldLeft == oldRight) continue
            val newTypes = Type.bottom(oldLeft, oldRight)
            changed = true
            insn.definitions.types = newTypes
            insn.expression.types = newTypes
        }
        return changed
    }

    private fun propagateVars(f: Function): Boolean {
        var changed = false
        val vars = findVars(f)
        val types = HashMap<Element.Variable, Type>()
        for (v in vars) {
            types.compute(v) { _, u -> if (u == null) v.type else Type.bottom(u, v.type) }
        }
        for (v in vars) {
            val newType = types.getValue(v)
            if (newType != v.type) {
                changed = true
                v.type = newType
            }
        }
        return changed
    }

    private fun findVars(f: Function): List<Element.Variable> {
        val list = ArrayList<Element.Variable>()
        list.addAll(f.arguments)
        for (insn in f.instructions) {
            if (insn !is Instruction.Evaluation) continue
            if (insn is Instruction.Assignment) {
                list.addAll(insn.definitions.list())
            }
            for (e in insn.expression.list<Expression>()) {
                if (e is Element.Variable) {
                    list.add(e)
                }
                if (e is Expression.Operation) {
                    for (ea in e.arguments.list<Expression>()) {
                        if (ea is Element.Variable) {
                            list.add(ea)
                        }
                    }
                }
            }
        }
        return list
    }

    private fun propagateComparisons(f: Function): Boolean {
        var changed = false
        for (insn in f.instructions) {
            if (insn !is Instruction.Branch) continue
            val operation = insn.expression as Expression.Operation
            val args = operation.arguments.list<Element>()
            val arg1 = args[0]
            val arg2 = args[1]
            if (arg1.type == arg2.type) continue
            val newType = Type.bottom(arg1.type, arg2.type)
            if (newType == Type.COLOUR && operation.id != Opcodes.BRANCH_EQUALS && operation.id != Opcodes.BRANCH_NOT) continue
            changed = true
            arg1.type = newType
            arg2.type = newType
        }
        return changed
    }

    private fun propagateReturns(f: Function): Boolean {
        var changed = false
        val returns = findReturns(f)
        val newTypes = returns.map { it.types }.reduce { acc, types -> Type.bottom(acc, types) }
        f.returnTypes = newTypes
        for (r in returns) {
            if (r.types != newTypes) {
                changed = true
                r.types = newTypes
            }
        }
        return changed
    }

    private fun findReturns(f: Function): List<Expression> {
        val list = ArrayList<Expression>()
        for (insn in f.instructions) {
            if (insn is Instruction.Return) {
                list.add(insn.expression)
            }
        }
        return list
    }
}