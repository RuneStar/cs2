package org.runestar.cs2.ir1

interface Insn {

    val line: Int

    val successors: IntArray

    class Statement(override val line: Int, val targets: List<Arg.Var>, val op: Int, val args: List<Arg>) : Insn {

        override val successors: IntArray = intArrayOf(line + 1)

        override fun toString(): String {
            val sb = StringBuilder()
            if (targets.isNotEmpty()) {
                targets.joinTo(sb, ", ")
                sb.append(" = ")
            }
            sb.append(op).append('(')
            args.joinTo(sb, ", ")
            sb.append(')')
            return sb.toString()
        }
    }

    class Branch(override val line: Int, val left: Arg.Var, val condition: Int, val right: Arg.Var, val pc: Int) : Insn {

        override val successors: IntArray = intArrayOf(line + 1, pc)

        override fun toString(): String = "if ($left <$condition> $right) goto $pc"
    }

    class Goto(override val line: Int, val pc: Int) : Insn {

        override val successors: IntArray = intArrayOf(pc)

        override fun toString(): String = "goto $pc"
    }

    class Return(override val line: Int, val args: List<Arg.Var>) : Insn {

        override val successors get() = IntArray(0)

        override fun toString(): String = "return(${args.joinToString(", ")})"
    }

    class Switch(override val line: Int, val arg: Arg.Var, val map: Map<Int, Int>) : Insn {

        override val successors = map.values.toIntArray().plus(line + 1)

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("switch(").append(arg).append("):")
            for (e in map) {
                sb.append("\n\t\t").append(e.key).append(": ").append(e.value)
            }
            return sb.toString()
        }
    }
}