package org.runestar.cs2.cg

import org.runestar.cs2.Alias
import org.runestar.cs2.ArrayType
import org.runestar.cs2.ir.EventProperty
import org.runestar.cs2.Loader
import org.runestar.cs2.Opcodes.*
import org.runestar.cs2.Primitive
import org.runestar.cs2.StackType
import org.runestar.cs2.Trigger
import org.runestar.cs2.Type
import org.runestar.cs2.cfa.Construct
import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.VarSource
import org.runestar.cs2.ir.list
import org.runestar.cs2.loadNotNull
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
        append("if ").appendExpr(if0.condition).append(" {")
        indent {
            appendConstruct(if0.body)
        }
        nextLine()
        append('}')
        for (ifn in branches) {
            append(" else if ").appendExpr(ifn.condition).append(" {")
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
        append("while ").appendExpr(construct.condition).append(" {")
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
                append("case ").appendConstantInt(cases.next(), type)
                for (case in cases) {
                    append(", ").appendConstantInt(case, type)
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
            StackType.INT -> appendConstantInt(expr.value.int, expr.typing.finalType)
        }
    }

    private fun appendConstantInt(n: Int, type: Type) {
        if (type is ArrayType) {
            append(type.identifier).append(n)
            return
        }
        if (n == -1 && type != Primitive.INT) {
            append(null)
            return
        }
        when (type) {
            Alias.TYPE -> append(Type.of(n.toByte()).identifier)
            Primitive.COMPONENT -> append(n ushr 16).append(':').append(n and 0xFFFF)
            Primitive.BOOLEAN -> append(Loader.BOOLEAN_NAMES.loadNotNull(n))
            Primitive.COORD ->  {
                val plane = n ushr 28
                val x = (n ushr 14) and 0x3FFF
                val z = n and 0x3FFF
                append(plane).append('_')
                append((x / 64)).append('_')
                append((z / 64)).append('_')
                append((x and 0x3F)).append('_')
                append((z and 0x3F))
            }
            Primitive.GRAPHIC -> appendQuoteNamedInt(Loader.GRAPHIC_NAMES, n)
            Primitive.FONTMETRICS -> appendNamedInt(Loader.GRAPHIC_NAMES, n)
            Alias.COLOUR -> {
                if (n shr 24 != 0) error(n)
                when (n) {
                    0xFF0000 -> append("^red")
                    0x00FF00 -> append("^green")
                    0x0000FF -> append("^blue")
                    0xFFFF00 -> append("^yellow")
                    0xFF00FF -> append("^magenta")
                    0x00FFFF -> append("^cyan")
                    0xFFFFFF -> append("^white")
                    0x000000 -> append("^black")
                    else -> append("0x").append(n.toString(16).padStart(6, '0'))
                }
            }
            Primitive.INT -> {
                when (n) {
                    Int.MAX_VALUE -> append("^max_32bit_int")
                    Int.MIN_VALUE -> append("^min_32bit_int")
                    else -> append(n)
                }
            }
            Alias.KEY -> append("^key_").append(Loader.KEY_NAMES.loadNotNull(n))
            Primitive.STAT -> appendNamedInt(Loader.STAT_NAMES, n)
            Primitive.OBJ, Primitive.NAMEDOBJ -> appendSuffixNamedInt(Loader.OBJ_NAMES, n)
            Alias.IFTYPE -> append("^iftype_").append(Loader.IFTYPE_NAMES.loadNotNull(n))
            Alias.SETSIZE -> append("^setsize_").append(Loader.SETSIZE_NAMES.loadNotNull(n))
            Alias.SETPOSH -> append("^setpos_").append(Loader.SETPOSH_NAMES.loadNotNull(n))
            Alias.SETPOSV -> append("^setpos_").append(Loader.SETPOSV_NAMES.loadNotNull(n))
            Alias.SETTEXTALIGNH -> append("^settextalign_").append(Loader.SETTEXTALIGNH_NAMES.loadNotNull(n))
            Alias.SETTEXTALIGNV -> append("^settextalign_").append(Loader.SETTEXTALIGNV_NAMES.loadNotNull(n))
            Alias.VAR -> append("var").append(n)
            Primitive.INV -> appendNamedInt(Loader.INV_NAMES, n)
            Primitive.MAPAREA -> appendNamedInt(Loader.MAPAREA_NAMES, n)
            Alias.CHATTYPE -> append("^chattype_").append(Loader.CHATTYPE_NAMES.loadNotNull(n))
            Alias.PARAM -> appendNamedInt(Loader.PARAM_NAMES, n)
            Alias.BIT -> append('^').append(Loader.BOOLEAN_NAMES.loadNotNull(n))
            Alias.WINDOWMODE -> append("^windowmode_").append(Loader.WINDOWMODE_NAMES.loadNotNull(n))
            Alias.CLIENTTYPE -> append("^clienttype_").append(Loader.CLIENTTYPE_NAMES.loadNotNull(n))
            Primitive.LOC -> appendSuffixNamedInt(Loader.LOC_NAMES, n)
            Primitive.MODEL -> appendSuffixNamedInt(Loader.MODEL_NAMES, n)
            Primitive.STRUCT -> appendSuffixNamedInt(Loader.STRUCT_NAMES, n)
            Primitive.NPC -> appendSuffixNamedInt(Loader.NPC_NAMES, n)
            Primitive.SEQ -> appendSuffixNamedInt(Loader.SEQ_NAMES, n)
            Primitive.AREA, Primitive.MAPELEMENT, Primitive.CHAR -> error(n)
            else -> append(n)
        }
    }

    private fun appendSuffixNamedInt(nameLoader: Loader<String>, n: Int) {
        val name = nameLoader.load(n)
        if (name != null) append(name).append('_')
        append(n)
    }

    private fun appendQuoteNamedInt(nameLoader: Loader<String>, n: Int) {
        val name = nameLoader.load(n)
        if (name == null) append(n) else append('"').append(name).append('"')
    }

    private fun appendNamedInt(nameLoader: Loader<String>, n: Int) {
        val name = nameLoader.load(n)
        if (name == null) append(n) else append(name)
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
            append('(').appendExpr(args[0]).append(' ').append(branchInfix).append(' ').appendExpr(args[1]).append(')')
        }  else if (calcInfix != null) {
            val wasCalc = inCalc
            if (!inCalc) {
                append("calc")
                inCalc = true
            }
            append('(').appendExpr(args[0]).append(' ').append(calcInfix).append(' ').appendExpr(args[1]).append(')')
            inCalc = wasCalc
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
                ADD to '+',
                SUB to '-',
                MULTIPLY to '*',
                DIV to '/',
                MOD to '%',
                AND to '&',
                OR to '|'
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