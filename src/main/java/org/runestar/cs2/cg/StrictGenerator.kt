package org.runestar.cs2.cg

import org.runestar.cs2.ArrayType
import org.runestar.cs2.ir.EventProperty
import org.runestar.cs2.Loader
import org.runestar.cs2.Opcodes.*
import org.runestar.cs2.StackType
import org.runestar.cs2.Trigger
import org.runestar.cs2.cfa.Construct
import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.VarSource
import org.runestar.cs2.ir.list
import org.runestar.cs2.names

fun StrictGenerator(writer: (scriptName: String, script: String) -> Unit) = object : StrictGenerator() {
    override fun write(scriptName: String, script: String) = writer(scriptName, script)
}

abstract class StrictGenerator : Generator {

    final override fun write(f: Function, root: Construct) {
        val name = Loader.SCRIPT_NAMES.load(f.id)?.toString() ?: "script${f.id}"
        write(name, Writer(name, f, root).write())
    }

    abstract fun write(scriptName: String, script: String)
}

private class Writer(
        private val name: String,
        private val f: Function,
        private val root: Construct
) {

    private val buf = StringBuilder()

    private var indents = 0

    private var inCalc = false

    private var endingLine = true

    private val definedLocalVariables = HashSet<Element.Variable>(f.arguments)

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
            val returnTypes = f.returnTypes.iterator()
            append(returnTypes.next().finalType.literal)
            for (returnType in returnTypes) {
                append(", ").append(returnType.finalType.literal)
            }
            append(')')
        }
        appendConstruct(root)
        if (endingLine) nextLine()
        return buf.toString()
    }

    private fun appendArg(arg: Element.Variable) {
        append(arg.typing.finalType.literal).append(' ').appendVar(arg)
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
        val type = construct.expression.typings.single().finalType
        append("switch_").append(type.literal).append(" (").appendExpr(construct.expression).append(") {")
        for ((ns, body) in construct.cases) {
            indent {
                nextLine()
                val cases = ns.iterator()
                append("case ").append(intValueToString(cases.next(), type))
                for (case in cases) {
                    append(", ").append(intValueToString(case, type))
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
                    val es = insn.expression.list<Expression>()
                    if (es.isNotEmpty()) {
                        append('(').appendExprs(es).append(')')
                    }
                }
            }
        }
        append(';')
    }

    private fun appendAssignment(insn: Instruction.Assignment) {
        val defs = insn.definitions.list<Element.Variable>()
        if (defs.isNotEmpty()) {
            if (defs.size == 1) {
                val def = defs.single()
                if (def.varId.source.local && definedLocalVariables.add(def)) {
                    append("def_").append(def.typing.finalType.literal).append(' ')
                }
                appendVar(def)
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
            is Element.Variable -> appendVar(expr)
            is Element.Constant -> appendConst(expr)
            is Expression.Operation.AddHook -> appendHook(expr)
            is Expression.Operation.Invoke -> appendInvoke(expr)
            is Expression.Operation -> appendOperation(expr)
            is Expression.Compound -> appendExprs(expr.expressions)
        }
    }

    private fun appendVar(v: Element.Variable) = apply {
        when (v.varId.source) {
            VarSource.LOCALINT, VarSource.LOCALSTRING, VarSource.ARRAY -> append('$').append(v.typing.finalType.identifier).append(v.varId.id)
            VarSource.VARP -> append("%var").append(v.varId.id)
            VarSource.VARBIT -> append("%varbit").append(v.varId.id)
            VarSource.VARCINT -> append("%varcint").append(v.varId.id)
            VarSource.VARCSTRING -> append("%varcstring").append(v.varId.id)
            else -> error(v)
        }
    }

    private fun appendConst(expr: Element.Constant) {
        when (expr.value.type) {
            StackType.STRING -> append('"').append(expr.value.string).append('"')
            StackType.INT -> append(intValueToString(expr.value.int, expr.typing.finalType))
        }
    }

    private fun appendOperation(expr: Expression.Operation) {
        val args = expr.arguments.list<Expression>()
        val opcode = expr.id
        when (opcode) {
            DEFINE_ARRAY -> {
                val array = args[0] as Element.Variable
                val elemType = (array.typing.type as ArrayType).elementType
                append("def_").append(elemType.literal).append(' ').appendVar(array).append('(').appendExpr(args[1]).append(')')
                return
            }
            PUSH_ARRAY_INT -> {
                appendVar(args[0] as Element.Variable).append('(').appendExpr(args[1]).append(')')
                return
            }
            POP_ARRAY_INT -> {
                appendVar(args[0] as Element.Variable).append('(').appendExpr(args[1]).append(") = ").appendExpr(args[2])
                return
            }
            JOIN_STRING -> {
                append('"')
                for (a in args) {
                    if (a is Element.Constant && a.value.type == StackType.STRING) {
                        append(a.value.string)
                    } else {
                        append('<').appendExpr(a).append('>')
                    }
                }
                append('"')
                return
            }
        }
        val branchInfix = BRANCH_INFIX_MAP[expr.id]
        val calcInfix = CALC_INFIX_MAP[expr.id]
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
            var args2: List<Expression> = args
            if (expr.id in DOT_OPCODES) {
                if ((args2.last() as Element.Constant).value.int == 1) {
                    append('.')
                }
                args2 = args2.dropLast(1)
            }
            append(names.getValue(opcode))
            if (args2.isNotEmpty()) {
                append('(').appendExprs(args2).append(')')
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
        val opPrec = PRECEDENCE_MAP[op.id] ?: error("Missing precedence for binary operation: ${op.id}")
        val exprPrec = if (expr is Expression.Operation) PRECEDENCE_MAP[expr.id] ?: 0 else 0
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

    private fun appendHook(operation: Expression.Operation.AddHook) {
        val args = operation.arguments.list<Expression>().toMutableList()
        val component = args.removeAt(args.lastIndex)

        if (operation.id < 2000 && (component as Element.Constant).value.int == 1) {
            append('.')
        }
        append(names.getValue(operation.id)).append('(')

        if (operation.scriptId == -1) {
            append(null)
        } else {
            val scriptName = Loader.SCRIPT_NAMES.load(operation.scriptId)
            append('"')
            if (scriptName == null) {
                append("script").append(operation.scriptId)
            } else {
                require(scriptName.trigger == Trigger.clientscript) { "$scriptName must be a ${Trigger.clientscript}" }
                append(scriptName.name)
            }
            val triggerCount = (args.removeAt(args.lastIndex) as Element.Constant).value.int
            val triggers = args.takeLast(triggerCount)
            repeat(triggerCount) { args.removeAt(args.lastIndex) }

            if (args.isNotEmpty()) {
                append('(').appendExprs(args).append(')')
            }

            if (triggers.isNotEmpty()) {
                append('{').appendExprs(triggers).append('}')
            }
            append('"')
        }
        if (operation.id >= 2000) {
            append(", ").appendExpr(component)
        }
        append(')')
    }

    private fun appendInvoke(invoke: Expression.Operation.Invoke) {
        val args = invoke.arguments.list<Expression>()
        append('~')
        val scriptName = Loader.SCRIPT_NAMES.load(invoke.scriptId)
        if (scriptName == null) {
            append("script").append(invoke.scriptId)
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

    private fun append(b: Boolean) = apply { buf.append(b) }

    private companion object {

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

        val DOT_OPCODES = setOf(
                CC_CREATE,
                CC_DELETE,
                CC_FIND,
                IF_FIND,
                CC_SETPOSITION,
                CC_SETSIZE,
                CC_SETHIDE,
                CC_SETNOCLICKTHROUGH,
                CC_SETNOSCROLLTHROUGH,
                CC_SETSCROLLPOS,
                CC_SETCOLOUR,
                CC_SETFILL,
                CC_SETTRANS,
                CC_SETLINEWID,
                CC_SETGRAPHIC,
                CC_SET2DANGLE,
                CC_SETTILING,
                CC_SETMODEL,
                CC_SETMODELANGLE,
                CC_SETMODELANIM,
                CC_SETMODELORTHOG,
                CC_SETTEXT,
                CC_SETTEXTFONT,
                CC_SETTEXTALIGN,
                CC_SETTEXTSHADOW,
                CC_SETOUTLINE,
                CC_SETGRAPHICSHADOW,
                CC_SETVFLIP,
                CC_SETHFLIP,
                CC_SETSCROLLSIZE,
                CC_RESUME_PAUSEBUTTON,
                _1122,
                CC_SETFILLCOLOUR,
                _1124,
                _1125,
                CC_SETLINEDIRECTION,
                CC_SETMODELTRANSPARENT,
                CC_SETOBJECT,
                CC_SETNPCHEAD,
                CC_SETPLAYERHEAD_SELF,
                CC_SETOBJECT_NONUM,
                CC_SETOBJECT_ALWAYS_NUM,
                CC_SETOP,
                CC_SETDRAGGABLE,
                CC_SETDRAGGABLEBEHAVIOR,
                CC_SETDRAGDEADZONE,
                CC_SETDRAGDEADTIME,
                CC_SETOPBASE,
                CC_SETTARGETVERB,
                CC_CLEAROPS,
                _1308,
                CC_SETOPKEY,
                CC_SETOPTKEY,
                CC_SETOPKEYRATE,
                CC_SETOPTKEYRATE,
                CC_SETOPKEYIGNOREHELD,
                CC_SETOPTKEYIGNOREHELD,
                CC_GETX,
                CC_GETY,
                CC_GETWIDTH,
                CC_GETHEIGHT,
                CC_GETHIDE,
                CC_GETLAYER,
                CC_GETSCROLLX,
                CC_GETSCROLLY,
                CC_GETTEXT,
                CC_GETSCROLLWIDTH,
                CC_GETSCROLLHEIGHT,
                CC_GETMODELZOOM,
                CC_GETMODELANGLE_X,
                CC_GETMODELANGLE_Z,
                CC_GETMODELANGLE_Y,
                CC_GETTRANS,
                _1610,
                CC_GETCOLOUR,
                CC_GETFILLCOLOUR,
                _1613,
                CC_GETMODELTRANSPARENT,
                CC_GETINVOBJECT,
                CC_GETINVCOUNT,
                CC_GETID,
                CC_GETTARGETMASK,
                CC_GETOP,
                CC_GETOPBASE,
                IF_CALLONRESIZE,
                CC_DRAGPICKUP,
                _3140
        )
    }
}