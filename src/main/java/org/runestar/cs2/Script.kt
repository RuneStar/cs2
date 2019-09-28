package org.runestar.cs2

import org.runestar.cs2.Opcodes.*
import org.runestar.cs2.util.toUnsignedInt
import java.nio.ByteBuffer

data class Script(
        val localIntCount: Int,
        val localStringCount: Int,
        val intArgumentCount: Int,
        val stringArgumentCount: Int,
        val operands: Array<Any>,
        val opcodes: ShortArray,
        val switches: Array<Map<Int, Int>>
) {

    val returnTypes: List<Type> = run {
        val ts = ArrayList<Type>()
        var i = opcodes.size - 2
        out@
        while (i >= 0) {
            when (opcodes[i--].toInt()) {
                PUSH_CONSTANT_INT -> ts.add(Primitive.INT)
                PUSH_CONSTANT_STRING -> ts.add(Primitive.STRING)
                else -> break@out
            }
        }
        ts.reverse()
        ts
    }

    companion object {

        fun read(bytes: ByteArray): Script = read(ByteBuffer.wrap(bytes))

        fun read(buffer: ByteBuffer): Script {
            check(buffer.get() == 0.toByte())
            val start = buffer.position()
            buffer.position(buffer.limit() - buffer.getShort(buffer.limit() - 2) - 14)

            val instructionCount = buffer.int
            val localIntCount = buffer.short.toUnsignedInt()
            val localStringCount = buffer.short.toUnsignedInt()
            val intArgumentCount = buffer.short.toUnsignedInt()
            val stringArgumentCount = buffer.short.toUnsignedInt()

            val switches = Array<Map<Int, Int>>(buffer.get().toUnsignedInt()) {
                val caseCount = buffer.short.toUnsignedInt()
                val switch = LinkedHashMap<Int, Int>(caseCount)
                repeat(caseCount) {
                    switch[buffer.int] = buffer.int
                }
                switch
            }

            buffer.position(start)

            val opcodes = ShortArray(instructionCount)
            val operands = Array<Any>(instructionCount) {
                val opcodeShort = buffer.short
                opcodes[it] = opcodeShort
                val opcode = opcodeShort.toUnsignedInt()
                when {
                    opcode >= 100 || opcode == RETURN || opcode == POP_INT_DISCARD || opcode == POP_STRING_DISCARD -> buffer.get().toUnsignedInt()
                    opcode == PUSH_CONSTANT_STRING -> buffer.readString()
                    else -> buffer.int
                }
            }

            buffer.position(buffer.limit())

            return Script(
                    localIntCount,
                    localStringCount,
                    intArgumentCount,
                    stringArgumentCount,
                    operands,
                    opcodes,
                    switches
            )
        }

        val CHARSET = charset("windows-1252")

        private fun ByteBuffer.readString(): String {
            val origPos = position()
            var length = 0
            while (get() != 0.toByte()) length++
            if (length == 0) return ""
            val byteArray = ByteArray(length)
            position(origPos)
            get(byteArray)
            position(position() + 1)
            return String(byteArray, CHARSET)
        }
    }
}