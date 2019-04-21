package org.runestar.cs2.ir

import org.runestar.cs2.Type
import org.runestar.cs2.util.Chain

class Func(
        val id: Int,
        val args: MutableList<Expr.Variable.Local>,
        var insns: Chain<Insn>,
        var returns: MutableList<Type>
) {

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("args=").appendln(args)
        insns.forEachIndexed { index, insn ->
            sb.appendln(insn)
        }
        sb.append("returns=").append(returns)
        return sb.toString()
    }
}