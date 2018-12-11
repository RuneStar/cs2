package org.runestar.cs2

import org.runestar.cs2.util.readString
import org.runestar.cs2.util.toUnsignedInt
import java.nio.ByteBuffer

data class Script(
        val localIntCount: Int,
        val localStringCount: Int,
        val intArgumentCount: Int,
        val stringArgumentCount: Int,
        val intOperands: IntArray,
        val stringOperands: Array<String?>,
        val opcodes: ShortArray,
        val switches: Array<Map<Int, Int>>
) {

    companion object {

        fun read(buffer: ByteBuffer): Script {
            val start = buffer.position()
            val end = buffer.limit()
            val mid = end - 2 - buffer.getShort(end - 2) - 12
            buffer.position(mid)
            val instructionCount = buffer.int
            val localIntCount = buffer.short.toUnsignedInt()
            val localStringCount = buffer.short.toUnsignedInt()
            val stackIntCount = buffer.short.toUnsignedInt()
            val stackStringCount = buffer.short.toUnsignedInt()
            val switches = Array<Map<Int, Int>>(buffer.get().toUnsignedInt()) {
                val switch = HashMap<Int, Int>()
                repeat(buffer.short.toUnsignedInt()) {
                    switch[buffer.int] = buffer.int
                }
                switch
            }

            buffer.position(start)
            buffer.readString()

            val opcodes = ShortArray(instructionCount)
            val intOperands = IntArray(instructionCount)
            val stringOperands = arrayOfNulls<String>(instructionCount)

            var i = 0
            while (buffer.position() < mid) {
                val opcodeShort = buffer.short
                opcodes[i] = opcodeShort
                val opcode = opcodeShort.toUnsignedInt()
                if (opcode == 3) {
                    stringOperands[i] = buffer.readString()
                } else if (opcode < 100 && opcode != 21 && opcode != 38 && opcode != 39) {
                    intOperands[i] = buffer.int
                } else {
                    intOperands[i] = buffer.get().toUnsignedInt()
                }
                i++
            }

            return Script(
                    localIntCount,
                    localStringCount,
                    stackIntCount,
                    stackStringCount,
                    intOperands,
                    stringOperands,
                    opcodes,
                    switches
            )
        }
    }
}