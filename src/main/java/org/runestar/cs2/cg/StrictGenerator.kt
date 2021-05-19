package org.runestar.cs2.cg

import org.runestar.cs2.ir.EventProperty
import org.runestar.cs2.bin.*
import org.runestar.cs2.SCRIPT_NAMES
import org.runestar.cs2.bin.StackType
import org.runestar.cs2.bin.Trigger
import org.runestar.cs2.cfa.Construct
import org.runestar.cs2.bin.int
import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.FunctionSet
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.Variable
import org.runestar.cs2.ir.asList
import org.runestar.cs2.ir.prototype
import org.runestar.cs2.bin.string
import org.runestar.cs2.ir.identifier
import org.runestar.cs2.ir.literal

fun StrictGenerator(writer: (scriptId: Int, scriptName: String, script: String) -> Unit) = object : StrictGenerator() {
    override fun write(scriptId: Int, scriptName: String, script: String) = writer(scriptId, scriptName, script)
}

abstract class StrictGenerator : Generator {

    final override fun write(f: Function, fs: FunctionSet, root: Construct) {
        var scriptName = SCRIPT_NAMES.load(f.id)
        if (scriptName == null) {
            var trigger = fs.callGraph.triggers[f.id]
            if (trigger == null) {
                trigger = if (f.returnTypes.isNotEmpty()) Trigger.proc else Trigger.clientscript
            }
            scriptName = ScriptName(trigger, "script${f.id}")
        }
        val name = scriptName.toString()
        write(f.id, name, Writer(name, f, fs, root).write())
    }

    abstract fun write(scriptId: Int, scriptName: String, script: String)
}

private class Writer(
        private val name: String,
        private val f: Function,
        private val fs: FunctionSet,
        private val root: Construct,
) {

    private val buf = StringBuilder()

    private var indents = 0

    private var inCalc = false

    private var endingLine = true

    private val definedLocalVariables = HashSet<Variable>(f.arguments)

    fun write(): String {
        append("// ").append(f.id).nextLine()
        append(name)
        if (f.arguments.isNotEmpty()) {
            append('(')
            val args = f.arguments.iterator()
            appendArg(args.next())
            for (arg in args) {
                append(", ").appendArg(arg)
            }
            append(')')
        } else if (f.returnTypes.isNotEmpty()) {
            append("()")
        }
        if (f.returnTypes.isNotEmpty()) {
            append('(')
            val returnTypes = fs.typings.returns(f.id, f.returnTypes).iterator()
            append(returnTypes.next().literal)
            for (returnType in returnTypes) {
                append(", ").append(returnType.literal)
            }
            append(')')
        }
        appendConstruct(root)
        if (endingLine) nextLine()
        return buf.toString()
    }

    private fun appendArg(arg: Variable) {
        append(fs.typings.of(arg).literal)
        if (arg is Variable.array) {
            append("array")
        }
        append(" $").appendVarIdentifier(arg)
    }

    private fun appendConstruct(construct: Construct) {
        when (construct) {
            is Construct.Seq -> appendSeq(construct)
            is Construct.If -> appendIf(construct)
            is Construct.While -> appendWhile(construct)
            is Construct.Switch -> appendSwitch(construct)
        }
    }

    private fun appendSeq(construct: Construct.Seq) {
        for (insn in construct.instructions) {
            nextLine()
            appendInsn(insn)
        }
        construct.next?.let { appendConstruct(it) }
    }

    private fun appendIf(construct: Construct.If) {
        nextLine()
        val branches = construct.branches.iterator()
        val if0 = branches.next()
        append("if (").appendExpr(if0.condition).append(") {")
        indent {
            appendConstruct(if0.body)
        }
        nextLine()
        append('}')
        for (ifn in branches) {
            append(" else if (").appendExpr(ifn.condition).append(") {")
            indent {
                appendConstruct(ifn.body)
            }
            nextLine()
            append('}')
        }
        val elze = construct.elze
        if (elze != null) {
            append(" else {")
            indent {
                appendConstruct(elze)
            }
            nextLine()
            append('}')
        }
        construct.next?.let { appendConstruct(it) }
    }

    private fun appendWhile(construct: Construct.While) {
        nextLine()
        append("while (").appendExpr(construct.condition).append(") {")
        indent {
            appendConstruct(construct.body)
        }
        nextLine()
        append('}')
        construct.next?.let { appendConstruct(it) }
    }

    private fun appendSwitch(construct: Construct.Switch) {
        nextLine()
        val prototype = fs.typings.of(construct.expression).single().prototype
        append("switch_").append(prototype.literal).append(" (").appendExpr(construct.expression).append(") {")
        for ((ns, body) in construct.cases) {
            indent {
                nextLine()
                val cases = ns.iterator()
                append("case ").append(intConstantToString(cases.next(), prototype))
                for (case in cases) {
                    append(", ").append(intConstantToString(case, prototype))
                }
                append(" :")
                indent {
                    appendConstruct(body)
                }
            }
        }
        val default = construct.default
        if (default != null) {
            indent {
                nextLine()
                append("case default :")
                indent {
                    appendConstruct(default)
                }
            }
        }
        nextLine()
        append('}')
        construct.next?.let { appendConstruct(it) }
    }

    private fun appendInsn(insn: Instruction) {
        when (insn) {
            is Instruction.Assignment -> appendAssignment(insn)
            is Instruction.Return -> {
                if (indents == 0 && f.returnTypes.isEmpty()) {
                    endingLine = false
                    return
                } else {
                    append("return")
                    val es = insn.expression.asList
                    if (es.isNotEmpty()) {
                        append('(').appendExprs(es).append(')')
                    }
                }
            }
        }
        append(';')
    }

    private fun appendAssignment(insn: Instruction.Assignment) {
        val defs = insn.definitions.asList as List<Element.Access>
        if (defs.isNotEmpty()) {
            if (defs.size == 1) {
                val def = defs.single()
                if (def.variable is Variable.Local && definedLocalVariables.add(def.variable)) {
                    append("def_").append(fs.typings.of(def.variable).literal).append(' ')
                }
                appendVarAccess(def)
            } else {
                appendExprs(defs)
            }
            append(" = ")
        }
        appendExpr(insn.expression)
    }

    private fun appendExpr(expr: Expression): Writer = apply {
        when (expr) {
            is EventProperty -> append(expr.literal)
            is Element.Access -> appendVarAccess(expr)
            is Element.Pointer -> appendVarIdentifier(expr.variable)
            is Element.Constant -> appendConst(expr)
            is Expression.ClientScript -> appendClientScript(expr)
            is Expression.Proc -> appendProc(expr)
            is Expression.Operation -> appendOperation(expr)
            is Expression.Compound -> appendExprs(expr.expressions)
        }
    }

    private fun appendVarAccess(e: Element.Access) = apply {
        when (e.variable) {
            is Variable.Local -> append('$')
            is Variable.Global -> append('%')
            else -> error(e)
        }
        appendVarIdentifier(e.variable)
    }

    private fun appendVarIdentifier(v: Variable) = apply {
        when (v) {
            is Variable.int, is Variable.string, is Variable.array -> {
                val identifier = fs.typings.of(v).identifier
                append(identifier)
                if (v is Variable.array) {
                    append("array")
                }
                append(v.id)
            }
            is Variable.varp -> append("var").append(v.id)
            is Variable.varbit -> append("varbit").append(v.id)
            is Variable.varcint -> append("varcint").append(v.id)
            is Variable.varcstring -> append("varcstring").append(v.id)
            is Variable.varclansetting -> append("varclansetting").append(v.id)
            is Variable.varclan -> append("varclan").append(v.id)
            else -> error(v)
        }
    }

    private fun appendConst(const: Element.Constant) {
        when (const.value.stackType) {
            StackType.STRING -> append('"').append(const.value.string).append('"')
            StackType.INT -> append(intConstantToString(const.value.int, fs.typings.of(const).prototype))
        }
    }

    private fun appendOperation(expr: Expression.Operation) {
        val args = expr.arguments.asList
        val opcode = expr.opcode
        when (opcode) {
            DEFINE_ARRAY -> {
                val array = args[0] as Element.Access
                append("def_").append(fs.typings.of(array).literal).append(' ').appendVarAccess(array).append('(').appendExpr(args[1]).append(')')
                return
            }
            PUSH_ARRAY_INT -> {
                appendVarAccess(args[0] as Element.Access).append('(').appendExpr(args[1]).append(')')
                return
            }
            POP_ARRAY_INT -> {
                appendVarAccess(args[0] as Element.Access).append('(').appendExpr(args[1]).append(") = ").appendExpr(args[2])
                return
            }
            JOIN_STRING -> {
                append('"')
                for (a in args) {
                    if (a is Element.Constant && a.value.stackType == StackType.STRING) {
                        append(a.value.string)
                    } else {
                        append('<').appendExpr(a).append('>')
                    }
                }
                append('"')
                return
            }
        }
        val branchInfix = BRANCH_INFIX_MAP[expr.opcode]
        val calcInfix = CALC_INFIX_MAP[expr.opcode]
        if (branchInfix != null) {
            appendBinaryOperation(args[0], expr, args[1], branchInfix)
        } else if (calcInfix != null) {
            val wasCalc = inCalc
            if (!inCalc) {
                append("calc(")
                inCalc = true
            }
            appendBinaryOperation(args[0], expr, args[1], calcInfix)
            inCalc = wasCalc
            if (!inCalc) append(')')
        } else {
            if (expr.dot) {
                append('.')
            }
            append(expr.opcodeName)
            if (args.isNotEmpty()) {
                append('(').appendExprs(args).append(')')
            }
        }
    }

    private fun appendBinaryOperation(lhs: Expression, op: Expression.Operation, rhs: Expression, opText: String) {
        appendExpressionPrec(lhs, op, false)
        append(' ')
        append(opText)
        append(' ')
        appendExpressionPrec(rhs, op, true)
    }

    private fun appendExpressionPrec(expr: Expression, op: Expression.Operation, assoc: Boolean) {
        val opPrec = PRECEDENCE_MAP[op.opcode] ?: error("Missing precedence for binary operation: ${op.opcode}")
        val exprPrec = if (expr is Expression.Operation) PRECEDENCE_MAP[expr.opcode] ?: 0 else 0
        val parenthesis = exprPrec > opPrec || (assoc && opPrec == exprPrec)
        if (parenthesis) append('(')
        appendExpr(expr)
        if (parenthesis) append(')')
    }

    private fun appendExprs(exprs: List<Expression>) = apply {
        if (exprs.isEmpty()) return this
        val es = exprs.iterator()
        appendExpr(es.next())
        for (e in es) {
            append(", ").appendExpr(e)
        }
    }

    private fun appendClientScript(cs: Expression.ClientScript) {
        val args = cs.arguments.asList
        val triggers = cs.triggers.asList
        val component = cs.component
        if (cs.dot) {
            append('.')
        }
        append(cs.opcodeName).append('(')

        if (cs.scriptId == -1) {
            append(null)
        } else {
            val scriptName = SCRIPT_NAMES.load(cs.scriptId)
            append('"')
            if (scriptName == null) {
                append("script").append(cs.scriptId)
            } else {
                require(scriptName.trigger == Trigger.clientscript) { "$scriptName must be a ${Trigger.clientscript}" }
                append(scriptName.name)
            }

            if (args.isNotEmpty()) {
                append('(').appendExprs(args).append(')')
            }
            if (triggers.isNotEmpty()) {
                append('{').appendExprs(triggers).append('}')
            }
            append('"')
        }
        if (component != null) {
            append(", ").appendExpr(component)
        }
        append(')')
    }

    private fun appendProc(proc: Expression.Proc) {
        val args = proc.arguments.asList
        append('~')
        val scriptName = SCRIPT_NAMES.load(proc.scriptId)
        if (scriptName == null) {
            append("script").append(proc.scriptId)
        } else {
            require(scriptName.trigger == Trigger.proc) { "$scriptName must be a ${Trigger.proc}" }
            append(scriptName.name)
        }
        if (args.isNotEmpty()) {
            append('(').appendExprs(args).append(')')
        }
    }

    private inline fun indent(action: () -> Unit) {
        indents++
        action()
        indents--
    }

    private fun nextLine() = apply {
        append('\n')
        repeat(indents) { append('\t') }
    }

    private fun append(s: String?) = apply { buf.append(s) }

    private fun append(c: Char) = apply { buf.append(c) }

    private fun append(n: Int) = apply { buf.append(n) }
}

val CALC_INFIX_MAP = mapOf(
        ADD to "+",
        SUB to "-",
        MULTIPLY to "*",
        DIV to "/",
        MOD to "%",
        AND to "&",
        OR to "|"
)

val BRANCH_INFIX_MAP = mapOf(
        BRANCH_EQUALS to "=",
        BRANCH_GREATER_THAN to ">",
        BRANCH_GREATER_THAN_OR_EQUALS to ">=",
        BRANCH_LESS_THAN to "<",
        BRANCH_LESS_THAN_OR_EQUALS to "<=",
        BRANCH_NOT to "!",
        SS_OR to "|",
        SS_AND to "&"
)

val PRECEDENCE_MAP = mapOf(
        // Calc ops
        MULTIPLY to 1,
        DIV to 1,
        MOD to 1,
        ADD to 2,
        SUB to 2,
        // Branch ops
        BRANCH_GREATER_THAN to 3,
        BRANCH_GREATER_THAN_OR_EQUALS to 3,
        BRANCH_LESS_THAN to 3,
        BRANCH_LESS_THAN_OR_EQUALS to 3,
        BRANCH_EQUALS to 4,
        BRANCH_NOT to 4,
        AND to 5,
        OR to 6,
        SS_AND to 7,
        SS_OR to 8
)