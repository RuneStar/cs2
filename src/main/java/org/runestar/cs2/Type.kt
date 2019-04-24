package org.runestar.cs2

enum class Type(val desc: Char? = null) {

    INT('i'),
    STRING('s'),
    COMPONENT('I'),
    BOOLEAN('1'),
    OBJ('o'),
    ENUM('g'),
    STAT('S'),
    GRAPHIC('d'),
    INV('v'),
    MODEL('m'),
    COORD('c'),
    CATEGORY('y'),
    LOC('l'),
    AREA('R'),
    MAPAREA('`'),
    NAMEDOBJ('O'),
    FONTMETRICS('f'),
    CHAR('z'),
    STRUCT('J'),
    SYNTH('P'),

    TYPE,
    COLOUR,
    IFTYPE,
    SETSIZE,
    SETPOSH,
    SETPOSV,
    SETTEXTALIGNH,
    SETTEXTALIGNV,
    VAR,
    CHATTYPE,
    PARAM,
    BIT,
    ;

    val nameLiteral: String = name.toLowerCase()

    val typeLiteral: String get() = if (desc == null) INT.nameLiteral else nameLiteral

    val topType: Type get() = if (this == STRING) STRING else INT

    companion object {

        private val map = values().filter { it.desc != null }.associateBy { it.desc }

        fun of(desc: Char): Type = map.getValue(desc)

        fun of(desc: Int): Type = of(desc.toChar())

        fun bottom(a: Type, b: Type): Type {
            if (a == b) return a
            if (a.topType != b.topType) incompatibleTypes(a, b)
            if (a == a.topType) return b
            if (b == b.topType) return a
            if ((a == OBJ || a == NAMEDOBJ) && (b == OBJ || b == NAMEDOBJ)) return OBJ
            if ((a == FONTMETRICS || a == GRAPHIC) && (b == FONTMETRICS || b == GRAPHIC)) return FONTMETRICS
            incompatibleTypes(a, b)
        }

        fun bottom(a: List<Type>, b: List<Type>): List<Type> {
            if (a.size != b.size) incompatibleTypes(a, b)
            return List(a.size) { bottom(a[it], b[it]) }
        }

        private fun <T> incompatibleTypes(a: T, b: T): Nothing = throw IllegalArgumentException("Incompatible types: $a & $b")
    }
}