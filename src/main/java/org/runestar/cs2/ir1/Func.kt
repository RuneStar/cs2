package org.runestar.cs2.ir1

class Func(
        val intArgumentCount: Int,
        val stringArgumentCount: Int,
        val insns: List<Insn>,
        val intReturnCount: Int,
        val stringReturnCount: Int
) {

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("intArgs=").append(intArgumentCount).append(", strArgs=").appendln(stringArgumentCount)
        insns.forEachIndexed { index, insn ->
            sb.append(index).append('\t').appendln(insn)
        }
        sb.append("intReturns=").append(intReturnCount).append(", strReturns=").appendln(stringReturnCount)
        return sb.toString()
    }
}