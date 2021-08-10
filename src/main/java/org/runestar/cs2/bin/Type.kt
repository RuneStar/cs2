package org.runestar.cs2.bin

import org.runestar.cs2.util.CP1252
import org.runestar.cs2.util.toByte

enum class Type(desc: Char = 0.toChar()) {

    AREA('R'),
    BOOLEAN('1'),
    CATEGORY('y'),
    CHAR('z'),
    COMPONENT('I'),
    COORD('c'),
    ENUM('g'),
    FONTMETRICS('f'),
    GRAPHIC('d'),
    INT('i'),
    INTERFACE('a'),
    INV('v'),
    LOC('l'),
    MAPAREA('`'),
    MAPELEMENT('µ'),
    MODEL('m'),
    NAMEDOBJ('O'),
    NPC('n'),
    OBJ('o'),
    PARAM,
    SEQ('A'),
    SPOTANIM('t'),
    STAT('S'),
    STRING('s'),
    STRUCT('J'),
    SYNTH('P'),
    NEWVAR('-'),
    NPC_UID('u'),
    PLAYER_UID('p'),
    TYPE,
    ;

    val desc = desc.toByte(CP1252)

    val stackType get() = if (this == STRING) StackType.STRING else StackType.INT

    val literal = name.toLowerCase()

    override fun toString() = literal

    companion object {

        private val VALUES = values().filter { it.desc != 0.toByte() }.associateBy { it.desc }

        fun of(desc: Byte): Type = VALUES.getValue(desc)

        fun ofAuto(desc: Byte): Type = if (desc == 0.toByte()) INT else of(desc)

        fun union(types: Set<Type>): Type? {
            when (types.size) {
                0 -> return null
                1 -> return types.iterator().next()
                2 -> {
                    if (OBJ in types && NAMEDOBJ in types) return OBJ
                    if (FONTMETRICS in types && GRAPHIC in types) return FONTMETRICS
                }
            }
            error(types)
        }

        fun intersection(types: Set<Type>): Type? {
            when (types.size) {
                0 -> return null
                1 -> return types.iterator().next()
                2 -> {
                    if (OBJ in types && NAMEDOBJ in types) return NAMEDOBJ
                    if (FONTMETRICS in types && GRAPHIC in types) return GRAPHIC
                }
            }
            error(types)
        }
    }
}