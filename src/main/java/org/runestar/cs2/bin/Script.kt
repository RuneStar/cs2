package org.runestar.cs2.bin

import org.runestar.cs2.util.CP1252
import org.runestar.cs2.util.toUnsignedInt
import java.nio.ByteBuffer

class Script(
        val localIntCount: Int,
        val localStringCount: Int,
        val intArgumentCount: Int,
        val stringArgumentCount: Int,
        val operands: Array<Value>,
        val opcodes: ShortArray,
        val switches: Array<Map<Int, Int>>,
) {

    val returnTypes: List<StackType> = run {
        val ts = ArrayList<StackType>()
        var i = opcodes.size - 2
        while (i >= 0) {
            when (opcodes[i--].toInt()) {
                PUSH_CONSTANT_INT -> ts.add(StackType.INT)
                PUSH_CONSTANT_STRING -> ts.add(StackType.STRING)
                else -> break
            }
        }
        ts.reverse()
        ts
    }
}

fun Script(bytes: ByteArray): Script = Script(ByteBuffer.wrap(bytes))

fun Script(buffer: ByteBuffer): Script {
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
    val operands = Array(instructionCount) {
        val opcodeShort = buffer.short
        opcodes[it] = opcodeShort
        val opcode = opcodeShort.toUnsignedInt()
        when {
            opcode >= 100 || opcode == RETURN || opcode == POP_INT_DISCARD || opcode == POP_STRING_DISCARD -> Value(buffer.get().toUnsignedInt())
            opcode == PUSH_CONSTANT_STRING -> Value(buffer.readString())
            else -> Value(buffer.int)
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

private fun ByteBuffer.readString(): String {
    val start = position()
    while (get() != 0.toByte());
    val len = position() - 1 - start
    if (len == 0) return ""
    val array: ByteArray
    var offset = 0
    if (hasArray()) {
        array = array()
        offset = arrayOffset() + start
    } else {
        array = ByteArray(len)
        position(start)
        get(array)
        position(position() + 1)
    }
    return String(array, offset, len, CP1252)
}