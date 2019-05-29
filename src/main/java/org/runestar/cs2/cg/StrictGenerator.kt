package org.runestar.cs2.cg

import org.runestar.cs2.ir.EventProperty
import org.runestar.cs2.Loader
import org.runestar.cs2.Opcodes.*
import org.runestar.cs2.Trigger
import org.runestar.cs2.Type
import org.runestar.cs2.cfa.Construct
import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Expression
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.list
import org.runestar.cs2.names

fun StrictGenerator(writer: (scriptName: String, script: String) -> Unit) = object : StrictGenerator() {
    override fun write(scriptName: String, script: String) = writer(scriptName, script)
}

abstract class StrictGenerator : Generator {

    final override fun write(f: Function, root: Construct) {
        val buf = StringBuilder()
        val state = State(buf, f, root)
        state.write()
        write(state.name, buf.toString())
    }

    abstract fun write(scriptName: String, script: String)
}

private class State(buf: StringBuilder, private val f: Function, private val root: Construct) {

    private var inCalc = false

    private var endingLine = true

    private val definedLocalVariables = HashSet<Element.Variable.Local>(f.arguments)

    private val writer = LineWriter(buf)

    lateinit var name: String

    internal fun write() {
        writer.append("// ").append(f.id)
        writer.nextLine()
        name = Loader.SCRIPT_NAMES.load(f.id)?.toString() ?: "script${f.id}"
        writer.append(name)
        if (f.arguments.isNotEmpty() || f.returnTypes.isNotEmpty()) {
            writer.append('(')
            f.arguments.joinTo(writer) { "${it.type.typeLiteral} \$${it.type.nameLiteral}${it.id}" }
            writer.append(')')
        }
        if (f.returnTypes.isNotEmpty()) {
            writer.append('(')
            f.returnTypes.joinTo(writer) { it.typeLiteral }
            writer.append(')')
        }
        writeConstruct(root)
        if (endingLine) writer.nextLine()
    }

    private fun writeConstruct(construct: Construct) {
        when (construct) {
            is Construct.Seq -> writeSeq(construct)
            is Construct.If -> writeIf(construct)
            is Construct.While -> writeWhile(construct)
            is Construct.Switch -> writeSwitch(construct)
        }
    }

    private fun writeSeq(construct: Construct.Seq) {
        for (insn in construct.instructions) {
            writer.nextLine()
            writeInsn(insn)
        }
        construct.next?.let { writeConstruct(it) }
    }

    private fun writeIf(construct: Construct.If) {
        writer.nextLine()
        val branches = construct.branches.iterator()
        val if0 = branches.next()
        writer.append("if ")
        writeExpr(if0.condition)
        writer.append(" {")
        writer.indents++
        writeConstruct(if0.body)
        writer.indents--
        writer.nextLine()
        writer.append('}')
        for (ifn in branches) {
            writer.append(" else if ")
            writeExpr(ifn.condition)
            writer.append(" {")
            writer.indents++
            writeConstruct(ifn.body)
            writer.indents--
            writer.nextLine()
            writer.append('}')
        }
        val elze = construct.elze
        if (elze != null) {
            writer.append(" else {")
            writer.indents++
            writeConstruct(elze)
            writer.indents--
            writer.nextLine()
            writer.append('}')
        }
        construct.next?.let { writeConstruct(it) }
    }

    private fun writeWhile(construct: Construct.While) {
        writer.nextLine()
        writer.append("while ")
        writeExpr(construct.condition)
        writer.append(" {")
        writer.indents++
        writeConstruct(construct.body)
        writer.indents--
        writer.nextLine()
        writer.append('}')
        construct.next?.let { writeConstruct(it) }
    }

    private fun writeSwitch(construct: Construct.Switch) {
        writer.nextLine()
        val type = construct.expression.types.single()
        writer.append("switch_").append(type.typeLiteral).append(" (")
        writeExpr(construct.expression)
        writer.append(") {")
        for ((ns, body) in construct.cases) {
            writer.indents++
            writer.nextLine()
            val itr = ns.iterator()
            writer.append("case ")
            writeConstantInt(itr.next(), type)
            while (itr.hasNext()) {
                writer.append(", ")
                writeConstantInt(itr.next(), type)
            }
            writer.append(" :")
            writer.indents++
            writeConstruct(body)
            writer.indents--
            writer.indents--
        }
        val default = construct.default
        if (default != null) {
            writer.indents++
            writer.nextLine()
            writer.append("case default :")
            writer.indents++
            writeConstruct(default)
            writer.indents--
            writer.indents--
        }
        writer.nextLine()
        writer.append('}')
        construct.next?.let { writeConstruct(it) }
    }

    private fun writeInsn(insn: Instruction) {
        when (insn) {
            is Instruction.Assignment -> writeAssignment(insn)
            is Instruction.Return -> {
                if (writer.indents == 0 && f.returnTypes.isEmpty()) {
                    endingLine = false
                    return
                } else {
                    writer.append("return")
                    val es = insn.expression.list<Expression>()
                    if (es.isNotEmpty()) {
                        writer.append('(')
                        writeExprList(es)
                        writer.append(')')
                    }
                }
            }
        }
        writer.append(';')
    }

    private fun writeAssignment(insn: Instruction.Assignment) {
        val defs = insn.definitions.list<Element.Variable>()
        if (defs.isNotEmpty()) {
            if (defs.size == 1) {
                val def = defs.single()
                if (def is Element.Variable.Local && definedLocalVariables.add(def)) {
                    writer.append("def_")
                    writer.append(def.type.typeLiteral)
                    writer.append(' ')
                }
                writeVar(def)
            } else {
                writeExprList(defs)
            }
            writer.append(" = ")
        }
        writeExpr(insn.expression)
    }

    private fun writeExpr(expr: Expression) {
        when (expr) {
            is EventProperty -> writer.append(expr.literal)
            is Element.Variable -> writeVar(expr)
            is Element.Constant -> writeConst(expr)
            is Expression.Operation.AddHook -> writeAddHook(expr)
            is Expression.Operation.Invoke -> writeInvoke(expr)
            is Expression.Operation -> writeOperation(expr)
            is Expression.Compound -> writeExprList(expr.expressions)
        }
    }

    private fun writeVar(v: Element.Variable) {
        when (v) {
            is Element.Variable.Local -> writer.append('$').append(v.type.nameLiteral).append(v.id)
            is Element.Variable.Varp -> writer.append("%var").append(v.id)
            is Element.Variable.Varbit -> writer.append("%varbit").append(v.id)
            is Element.Variable.Varc -> writer.append("%varc").append(v.type.topType.nameLiteral).append(v.id)
            else -> error(v)
        }
    }

    private fun writeConst(expr: Element.Constant) {
        if (expr.type == Type.STRING) {
            writer.append('"').append(expr.value as String).append('"')
        } else {
            writeConstantInt(expr.value as Int, expr.type)
        }
    }

    private fun writeConstantInt(n: Int, type: Type) {
        if (n == -1 && type != Type.INT) {
            writer.append(null)
            return
        }
        when (type) {
            Type.TYPE -> writer.append(Type.of(n).nameLiteral)
            Type.COMPONENT -> writer.append(n ushr 16).append(':').append(n and 0xFFFF)
            Type.BOOLEAN -> when (n) {
                0 -> writer.append(false)
                1 -> writer.append(true)
                else -> error(n)
            }
            Type.COORD ->  {
                val plane = n ushr 28
                val x = (n ushr 14) and 0x3FFF
                val z = n and 0x3FFF
                writer.append(plane).append('_')
                writer.append((x / 64)).append('_')
                writer.append((z / 64)).append('_')
                writer.append((x and 0x3F)).append('_')
                writer.append((z and 0x3F))
            }
            Type.GRAPHIC -> writeQuoteNamedInt(Loader.GRAPHIC_NAMES, n)
            Type.FONTMETRICS -> writeNamedInt(Loader.GRAPHIC_NAMES, n)
            Type.COLOUR -> {
                if (n shr 24 != 0) error(n)
                when (n) {
                    0xFF0000 -> writer.append("^red")
                    0x00FF00 -> writer.append("^green")
                    0x0000FF -> writer.append("^blue")
                    0xFFFF00 -> writer.append("^yellow")
                    0xFF00FF -> writer.append("^magenta")
                    0x00FFFF -> writer.append("^cyan")
                    0xFFFFFF -> writer.append("^white")
                    0x000000 -> writer.append("^black")
                    else -> writer.append("0x").append(n.toString(16).padStart(6, '0'))
                }
            }
            Type.INT -> {
                when (n) {
                    Int.MAX_VALUE -> writer.append("^max_32bit_int")
                    Int.MIN_VALUE -> writer.append("^min_32bit_int")
                    else -> writer.append(n)
                }
            }
            Type.KEY -> writer.append("^key_").append(checkNotNull(Loader.KEY_NAMES.load(n)))
            Type.CHAR -> error(n)
            Type.STAT -> writeNamedInt(Loader.STAT_NAMES, n)
            Type.OBJ, Type.NAMEDOBJ -> writeNamedInt(Loader.OBJ_NAMES, n)
            Type.IFTYPE -> {
                val s = when (n) {
                    3 -> "rectangle"
                    4 -> "text"
                    5 -> "graphic"
                    6 -> "model"
                    9 -> "line"
                    else -> error(n)
                }
                writer.append("^iftype_").append(s)
            }
            Type.SETSIZE -> {
                val s = when (n) {
                    0 -> "abs"
                    1 -> "minus"
                    2 -> "2"
                    else -> error(n)
                }
                writer.append("^setsize_").append(s)
            }
            Type.SETPOSH -> {
                val s = when (n) {
                    0 -> "abs_left"
                    1 -> "abs_centre"
                    2 -> "abs_right"
                    3 -> "3"
                    4 -> "4"
                    5 -> "5"
                    else -> error(n)
                }
                writer.append("^setpos_").append(s)
            }
            Type.SETPOSV -> {
                val s = when (n) {
                    0 -> "abs_top"
                    1 -> "abs_centre"
                    2 -> "abs_bottom"
                    3 -> "3"
                    4 -> "4"
                    5 -> "5"
                    else -> error(n)
                }
                writer.append("^setpos_").append(s)
            }
            Type.SETTEXTALIGNH -> {
                val s = when (n) {
                    0 -> "left"
                    1 -> "centre"
                    2 -> "right"
                    else -> error(n)
                }
                writer.append("^settextalign_").append(s)
            }
            Type.SETTEXTALIGNV -> {
                val s = when (n) {
                    0 -> "top"
                    1 -> "centre"
                    2 -> "bottom"
                    else -> error(n)
                }
                writer.append("^settextalign_").append(s)
            }
            Type.VAR -> writer.append("var").append(n)
            Type.INV -> writeNamedInt(Loader.INV_NAMES, n)
            Type.MAPAREA -> writeNamedInt(Loader.MAPAREA_NAMES, n)
            Type.CHATTYPE -> writer.append("^chattype_").append(checkNotNull(Loader.CHATTYPE_NAMES.load(n)))
            Type.PARAM -> writeNamedInt(Loader.PARAM_NAMES, n)
            Type.BIT -> {
                val s = when (n) {
                    0 -> "^false"
                    1 -> "^true"
                    else -> error(n)
                }
                writer.append(s)
            }
            Type.WINDOWMODE -> {
                val s = when (n) {
                    1 -> "fixed"
                    2 -> "resizable"
                    else -> error(n)
                }
                writer.append("^windowmode_").append(s)
            }
            else -> writer.append(n)
        }
    }

    private fun writeQuoteNamedInt(nameLoader: Loader<String>, n: Int) {
        val name = nameLoader.load(n)
        if (name == null) {
            writer.append(n)
        } else {
            writer.append('"').append(name).append('"')
        }
    }

    private fun writeNamedInt(nameLoader: Loader<String>, n: Int) {
        val name = nameLoader.load(n)
        if (name == null) {
            writer.append(n)
        } else {
            writer.append(name)
        }
    }

    private fun writeOperation(expr: Expression.Operation) {
        val args = expr.arguments.list<Expression>()
        val opcode = expr.id
        when (opcode) {
            DEFINE_ARRAY -> {
                writer.append("def_")
                writeExpr(args[1])
                writer.append(" \$array")
                writeExpr(args[0])
                writer.append('(')
                writeExpr(args[2])
                writer.append(')')
                return
            }
            PUSH_ARRAY_INT -> {
                writer.append("\$array")
                writeExpr(args[0])
                writer.append('(')
                writeExpr(args[1])
                writer.append(')')
                return
            }
            POP_ARRAY_INT -> {
                writer.append("\$array")
                writeExpr(args[0])
                writer.append('(')
                writeExpr(args[1])
                writer.append(") = ")
                writeExpr(args[2])
                return
            }
            JOIN_STRING -> {
                writer.append('"')
                for (a in args) {
                    if (a is Element.Constant && a.value is String) {
                        writer.append(a.value)
                    } else {
                        writer.append('<')
                        writeExpr(a)
                        writer.append('>')
                    }
                }
                writer.append('"')
                return
            }
        }
        val branchInfix = BRANCH_INFIX_MAP[expr.id]
        val calcInfix = CALC_INFIX_MAP[expr.id]
        if (branchInfix != null) {
            writer.append('(')
            writeExpr(args[0])
            writer.append(' ')
            writer.append(branchInfix)
            writer.append(' ')
            writeExpr(args[1])
            writer.append(')')
        }  else if (calcInfix != null) {
            val wasCalc = inCalc
            if (!inCalc) {
                writer.append("calc")
                inCalc = true
            }
            writer.append('(')
            writeExpr(args[0])
            writer.append(' ')
            writer.append(calcInfix)
            writer.append(' ')
            writeExpr(args[1])
            writer.append(')')
            inCalc = wasCalc
        } else {
            var args2: List<Expression> = args
            if (expr.id in DOT_OPCODES) {
                if ((args2.last() as Element.Constant).value == 1) {
                    writer.append('.')
                }
                args2 = args2.dropLast(1)
            }
            writer.append(names.getValue(opcode))
            if (args2.isNotEmpty()) {
                writer.append('(')
                writeExprList(args2)
                writer.append(')')
            }
        }
    }

    private fun writeExprList(exprs: List<Expression>) {
        if (exprs.isEmpty()) return
        val es = exprs.iterator()
        writeExpr(es.next())
        while (es.hasNext()) {
            writer.append(", ")
            writeExpr(es.next())
        }
    }

    private fun writeAddHook(operation: Expression.Operation.AddHook) {
        val args = operation.arguments.list<Expression>().toMutableList()
        val component = args.removeAt(args.lastIndex)

        if (operation.id < 2000 && (component as Element.Constant).value == 1) {
            writer.append('.')
        }
        writer.append(names[operation.id]).append('(')

        if (operation.scriptId == -1) {
            writer.append(null)
        } else {
            val scriptName = Loader.SCRIPT_NAMES.load(operation.scriptId)
            writer.append('"')
            if (scriptName == null) {
                writer.append("script").append(operation.scriptId)
            } else {
                require(scriptName.trigger == Trigger.clientscript) { "$scriptName must be a ${Trigger.clientscript}" }
                writer.append(scriptName.name)
            }
            val triggerCount = (args.removeAt(args.lastIndex) as Element.Constant).value as Int
            val triggers = args.takeLast(triggerCount)
            repeat(triggerCount) { args.removeAt(args.lastIndex) }

            writer.append('(')
            writeExprList(args)
            writer.append(')')

            if (triggers.isNotEmpty()) {
                writer.append('{')
                writeExprList(triggers)
                writer.append('}')
            }
            writer.append('"')
        }
        if (operation.id >= 2000) {
            writer.append(", ")
            writeExpr(component)
        }
        writer.append(')')
    }

    private fun writeInvoke(invoke: Expression.Operation.Invoke) {
        val args = invoke.arguments.list<Expression>()
        writer.append('~')
        val scriptName = Loader.SCRIPT_NAMES.load(invoke.scriptId)
        if (scriptName == null) {
            writer.append("script").append(invoke.scriptId)
        } else {
            require(scriptName.trigger == Trigger.proc) { "$scriptName must be a ${Trigger.proc}" }
            writer.append(scriptName.name)
        }
        if (args.isNotEmpty()) {
            writer.append('(')
            writeExprList(args)
            writer.append(')')
        }
    }

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

        val DOT_OPCODES = setOf(
                CC_CREATE,
                CC_DELETE,
                CC_FIND,
                IF_FIND,
                CC_SETPOSITION,
                CC_SETSIZE,
                CC_SETHIDE,
                CC_SETNOCLICKTHROUGH,
                _1006,
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
                _1127,
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
                _1614,
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