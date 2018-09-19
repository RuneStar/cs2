package org.runestar.cs2.ir

import org.runestar.cs2.util.Chain

class Func(
        val id: Int,
        val intArgumentCount: Int,
        val stringArgumentCount: Int,
        var insns: Chain<Insn>,
        val intReturnCount: Int,
        val stringReturnCount: Int
) {

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("intArgs=").append(intArgumentCount).append(", strArgs=").appendln(stringArgumentCount)
        insns.forEachIndexed { index, insn ->
            sb.appendln(insn)
        }
        sb.append("intReturns=").append(intReturnCount).append(", strReturns=").appendln(stringReturnCount)
        return sb.toString()
    }
}