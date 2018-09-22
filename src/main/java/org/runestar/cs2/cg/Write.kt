package org.runestar.cs2.cg

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type
import org.runestar.cs2.cfa.Construct
import org.runestar.cs2.cfa.reconstruct
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn
import org.runestar.cs2.names

fun write(
        appendable: Appendable,
        func: Func
) {
    val writer = LineWriter(appendable)
    val root = reconstruct(func)
    writer.append("script").append(func.id.toString()).append('(')
    func.args.joinTo(writer) { "${it.type.literal} \$${it.name}" }
    writer.append(")(")
    func.returns.joinTo(writer) { it.literal }
    writer.append(") {")
    writer.indents++
    writeConstruct(writer, root)
    writer.indents--
    writer.nextLine().append('}')
    writer.indents--
    writer.nextLine()
}

private fun writeConstruct(writer: LineWriter, construct: Construct) {
    when (construct) {
        is Construct.Seq -> writeSeq(writer, construct)
        is Construct.If -> writeIf(writer, construct)
        is Construct.While -> writeWhile(writer, construct)
        is Construct.Switch -> writeSwitch(writer, construct)
    }
}

private fun writeSeq(writer: LineWriter, construct: Construct.Seq) {
    for (insn in construct.insns) {
        writer.nextLine()
        writeInsn(writer, insn)
    }
    construct.next?.let { writeConstruct(writer, it) }
}

private fun writeIf(writer: LineWriter, construct: Construct.If) {
    writer.nextLine()
    val if0 = construct.branches.first()
    writer.append("if (")
    writeExpr(writer, if0.condition)
    writer.append(") {")
    writer.indents++
    writeConstruct(writer, if0.construct)
    writer.indents--
    writer.nextLine()
    writer.append('}')
    for (ifn in construct.branches.drop(1)) {
        writer.append(" else if (")
        writeExpr(writer, ifn.condition)
        writer.append(") {")
        writer.indents++
        writeConstruct(writer, ifn.construct)
        writer.indents--
        writer.nextLine()
        writer.append('}')
    }
    val elze = construct.elze
    if (elze != null) {
        writer.append(" else {")
        writer.indents++
        writeConstruct(writer, elze)
        writer.indents--
        writer.nextLine()
        writer.append('}')
    }
    construct.next?.let { writeConstruct(writer, it) }
}

private fun writeWhile(writer: LineWriter, construct: Construct.While) {
    writer.nextLine()
    writer.append("while (")
    writeExpr(writer, construct.condition)
    writer.append(") {")
    writer.indents++
    writeConstruct(writer, construct.inside)
    writer.indents--
    writer.nextLine()
    writer.append('}')
    construct.next?.let { writeConstruct(writer, it) }
}

private fun writeSwitch(writer: LineWriter, construct: Construct.Switch) {
    writer.nextLine()
    writer.append("switch (")
    writeExpr(writer, construct.expr)
    writer.append(") {")
    for ((n, con) in construct.map) {
        writer.indents++
        writer.nextLine()
        writer.append("case ").append(n.toString()).append(" {")
        writer.indents++
        writeConstruct(writer, con)
        writer.indents--
        writer.nextLine()
        writer.append('}')
        writer.indents--
    }
    writer.nextLine()
    writer.append('}')
    construct.next?.let { writeConstruct(writer, it) }
}

private fun writeInsn(writer: LineWriter, insn: Insn) {
    when (insn) {
        is Insn.Assignment -> writeAssignment(writer, insn)
        is Insn.Return -> writeReturn(writer, insn)
    }
}

private fun writeAssignment(writer: LineWriter, insn: Insn.Assignment) {
    val defs = insn.definitions.iterator()
    if (defs.hasNext()) {
        writeExpr(writer, defs.next())
    }
    while (defs.hasNext()) {
        writeExpr(writer, defs.next())
    }
    if (insn.definitions.isNotEmpty()) {
        writer.append(" = ")
    }
    writeExpr(writer, insn.expr)
}

private fun writeReturn(writer: LineWriter, insn: Insn.Return) {
    writeOperation(writer, insn.expr as Expr.Operation)
}

private fun writeExpr(writer: LineWriter, expr: Expr) {
    when (expr) {
        is Expr.Var -> writeVar(writer, expr)
        is Expr.Cst -> writeConst(writer, expr)
        is Expr.Operation -> writeOperation(writer, expr)
    }
}

private fun writeVar(writer: LineWriter, expr: Expr.Var) {
    writer.append('$').append(expr.name)
}

private fun writeConst(writer: LineWriter, expr: Expr.Cst) {
    when (expr.type) {
        Type.STRING -> writer.append('"').append(expr.cst.toString()).append('"')
        Type.TYPE -> writer.append(Type.of((expr.cst as Int).toChar()).literal)
        Type.COMPONENT -> {
            val n = expr.cst as Int
            writer.append("${n ushr 16}").append(':').append("${n and 0xFFFF}")
        }
        Type.BOOLEAN -> when (expr.cst as Int) {
            0 -> writer.append("false")
            1 -> writer.append("true")
            else -> error(expr)
        }
        else -> writer.append(expr.cst.toString())
    }
}

private fun writeOperation(writer: LineWriter, expr: Expr.Operation) {
    val op = expr.id
    val infixSym = INFIX_MAP[expr.id]
    if (infixSym != null) {
        val a = expr.arguments[0]
        if (a is Expr.Operation && a.id in INFIX_MAP) {
            writer.append('(')
            writeExpr(writer, a)
            writer.append(')')
        } else {
            writeExpr(writer, a)
        }

        writer.append(' ').append(infixSym).append(' ')

        val b = expr.arguments[1]
        if (b is Expr.Operation && b.id in INFIX_MAP) {
            writer.append('(')
            writeExpr(writer, b)
            writer.append(')')
        } else {
            writeExpr(writer, b)
        }

    } else {
        writer.append(names.getValue(op))
        writer.append('(')
        val args = expr.arguments.iterator()
        if (args.hasNext()) {
            writeExpr(writer, args.next())
        }
        while (args.hasNext()) {
            writer.append(", ")
            writeExpr(writer, args.next())
        }
        writer.append(')')
    }
}

private val INFIX_MAP = mapOf(
        Opcodes.ADD to "+",
        Opcodes.SUB to "-",
        Opcodes.MULTIPLY to "*",
        Opcodes.DIV to "/",
        Opcodes.MOD to "%",
        Opcodes.AND to "&",
        Opcodes.OR to "|",
        Opcodes.APPEND_NUM to "+",
        Opcodes.APPEND to "+",
        Opcodes.APPEND_CHAR to "+",
        Opcodes.BRANCH_EQUALS to "==",
        Opcodes.BRANCH_GREATER_THAN to ">",
        Opcodes.BRANCH_GREATER_THAN_OR_EQUALS to ">=",
        Opcodes.BRANCH_LESS_THAN to "<",
        Opcodes.BRANCH_LESS_THAN_OR_EQUALS to "<=",
        Opcodes.BRANCH_NOT to "!=",
        Opcodes.SS_OR to "||",
        Opcodes.SS_AND to "&&"
)