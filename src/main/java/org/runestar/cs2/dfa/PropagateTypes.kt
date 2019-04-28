package org.runestar.cs2.dfa

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type
import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.list
import org.runestar.cs2.util.removeFirst
import java.util.ArrayDeque

class PropagateTypes(val fs: Map<Int, Function>) {

    companion object : Phase {
        override fun transform(fs: Map<Int, Function>) = PropagateTypes(fs).transform()
    }

    private val invokes = HashMap<Function, MutableSet<Function>>()

    private val globalVars = HashMap<Element.Variable, MutableSet<Function>>()

    private val globalVarTypes = HashMap<Element.Variable, Type>()

    private val queue = LinkedHashSet(fs.values)

    fun transform() {
        fs.values.forEach {
            findInvokes(it)
            findGlobalVars(it)
        }
        for (f in queue) {
            preCheckInvokeArgs(f)
        }
        while (queue.isNotEmpty()) {
            prop(queue.removeFirst())
        }
    }

    private fun preCheckInvokeArgs(f: Function) {
        for (insn in f.instructions) {
            if (insn !is Instruction.Assignment) continue
            val e = insn.expression
            if (e is Expression.Operation.Scripted) {
                val y = fs[e.scriptId] ?: continue
                updateInvokeArguments(e.scriptArguments.types, y)
            }
        }
    }

    private fun findInvokes(f: Function) {
        for (insn in f.instructions) {
            if (insn !is Instruction.Evaluation) continue
            val e = insn.expression
            if (e !is Expression.Operation.Scripted) continue
            invokes.getOrPut(fs[e.scriptId] ?: continue) { HashSet() }.add(f)
        }
    }

    private fun findGlobalVars(f: Function) {
        for (insn in f.instructions) {
            if (insn !is Instruction.Evaluation) continue
            for (e in insn.expression.list<Expression>()) {
                addGlobalVariable(e, f)
            }
            if (insn is Instruction.Assignment) {
                addGlobalVariable(insn.definitions, f)
            }
        }
    }

    private fun addGlobalVariable(e: Expression, f: Function) {
        if (e !is Element.Variable) return
        if (e is Element.Variable.Stack || e is Element.Variable.Local) return
        globalVars.getOrPut(e) { HashSet() }.add(f)
        globalVarTypes[e] = e.type
    }

    private fun prop(f: Function) {
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
            val newTypes = Type.merge(oldLeft, oldRight)
            changed = true
            if (newTypes != oldLeft) setTypes(insn.definitions, newTypes)
            if (newTypes != oldRight) {
                setTypes(insn.expression, newTypes)
                val e = insn.expression
                if (e is Expression.Operation.Invoke) {
                    val y = fs[e.scriptId] ?: continue
                    if (y.returnTypes != newTypes) {
                        y.returnTypes = newTypes
                        updateInvokeReturnUsages(y)
                        queue.add(y)
                    }
                }
            }
        }
        return changed
    }

    private fun propagateVars(f: Function): Boolean {
        var changed = false
        val vars = findVars(f)
        val types = HashMap<Element.Variable, Type>()
        for (v in vars) {
            types.compute(v) { _, u -> if (u == null) v.type else Type.merge(u, v.type) }
        }
        for (v in vars) {
            val newType = types.getValue(v)
            if (newType != v.type) {
                changed = true
                setType(v, newType)
            }
        }
        if (changed) {
            updateInvokeArgUsages(f)
            for (insn in f.instructions) {
                if (insn !is Instruction.Assignment) continue
                val e = insn.expression
                if (e !is Expression.Operation.Scripted) continue
                val y = fs[e.scriptId] ?: continue
                updateInvokeArguments(e.scriptArguments.types, y)
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
            val newType = Type.merge(arg1.type, arg2.type)
            if (newType == Type.COLOUR && operation.id != Opcodes.BRANCH_EQUALS && operation.id != Opcodes.BRANCH_NOT) continue
            changed = true
            if (arg1.type !=  newType) setType(arg1, newType)
            if (arg2.type !=  newType) setType(arg2, newType)
        }
        return changed
    }

    private fun propagateReturns(f: Function): Boolean {
        var changed = false
        val returns = findReturns(f)
        val newTypes = returns.map { it.types }.fold(f.returnTypes) { acc, types -> Type.merge(acc, types) }
        if (newTypes != f.returnTypes) {
            f.returnTypes = newTypes
            updateInvokeReturnUsages(f)
        }
        for (r in returns) {
            if (r.types != newTypes) {
                changed = true
                setTypes(r, newTypes)
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

    private fun updateInvokeArguments(argTypes: List<Type>, f: Function) {
        val fTypes = f.arguments.map { it.type }
        if (fTypes == argTypes) return
        val newTypes = fixTypes(fTypes, argTypes)
        val i = ArrayDeque(f.arguments.filter { it.type != Type.STRING })
        val s = ArrayDeque(f.arguments.filter { it.type == Type.STRING })
        f.arguments = List(argTypes.size) {
            val newType = newTypes[it]
            val a = if (newType == Type.STRING) {
                s.removeFirst()
            } else {
                i.removeFirst()
            }
            a.type = newType
            a
        }
        queue.add(f)
        updateInvokeArgUsages(f)
    }

    private fun updateInvokeArgUsages(invoked: Function) {
        val argTypes = invoked.arguments.map { it.type }
        for (f in invokes[invoked] ?: return) {
            for (insn in f.instructions) {
                if (insn !is Instruction.Assignment) continue
                val e = insn.expression
                if (e !is Expression.Operation.Scripted) continue
                if (e.scriptId != invoked.id) continue
                if (setInvokeArgs(argTypes, e)) queue.add(f)
            }
        }
    }

    private fun updateInvokeReturnUsages(invoked: Function) {
        for (f in invokes.getValue(invoked)) {
            for (insn in f.instructions) {
                if (insn !is Instruction.Assignment) continue
                val e = insn.expression
                if (e !is Expression.Operation.Invoke) continue
                if (e.scriptId != invoked.id) continue
                if (insn.definitions.types != invoked.returnTypes) {
                    insn.definitions.types = invoked.returnTypes
                    queue.add(f)
                }
            }
        }
    }

    private fun updateGlobalVariable(v: Element.Variable) {
        for (f in globalVars.getValue(v)) {
            for (insn in f.instructions) {
                if (insn !is Instruction.Assignment) continue
                if (insn.definitions == v) {
                    val d = insn.definitions as Element.Variable
                    if (d.type != v.type) {
                        d.type = v.type
                        queue.add(f)
                    }
                } else if (insn.expression == v) {
                    val e = insn.expression as Element.Variable
                    if (e.type != v.type) {
                        e.type = v.type
                        queue.add(f)
                    }
                }
            }
        }
    }

    private fun setTypes(e: Expression, ts: List<Type>) {
        when (e) {
            is Element -> setType(e, ts.single())
            is Expression.Operation -> e.types = ts
            is Expression.Compound -> {
                for (i in ts.indices) {
                    setType(e.expressions[i] as Element, ts[i])
                }
            }
            else -> error(e)
        }
    }

    private fun setType(e: Element, t: Type) {
        e.type = t
        when (e) {
            is Element.Variable.Varp, is Element.Variable.Varc, is Element.Variable.Varbit -> {
                globalVarTypes[e as Element.Variable] = t
                updateGlobalVariable(e)
            }
        }
    }

    private fun setInvokeArgs(argTypes: List<Type>, e: Expression.Operation.Scripted): Boolean {
        val args = e.scriptArguments.list<Element>()
        val oldTypes = args.map { it.type }
        if (oldTypes == argTypes) return false
        val newTypes = fixTypes(argTypes, oldTypes)
        args.forEachIndexed { i, a ->
            setType(a, newTypes[i])
        }
        return true
    }

    private fun fixTypes(other: List<Type>, ordered: List<Type>): List<Type> {
        val i = ArrayDeque(Type.merge(other.filter { it.topType == Type.INT }, ordered.filter { it.topType == Type.INT }))
        val s = ArrayDeque(Type.merge(other.filter { it.topType == Type.STRING }, ordered.filter { it.topType == Type.STRING }))
        return List(ordered.size) {
            if (ordered[it] == Type.STRING) {
                s.removeFirst()
            } else {
                i.removeFirst()
            }
        }
    }
}