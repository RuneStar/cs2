package org.runestar.cs2

enum class TopType {

    INT, STRING;

    val type: Type get() = if (this == INT) Type.INT else Type.STRING
}

enum class Type(val desc: Char) {

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
    COORDGRID('c'),
    CATEGORY('y'),
    LOC('l'),
    AREA('R'),
    MAPAREA('`'),

    TYPE('?'),
    ;

    val literal = name.toLowerCase()

    val topType: TopType get() = if (this == STRING) TopType.STRING else TopType.INT

    companion object {

        private val map = values().associateBy { it.desc }

        fun of(desc: Char): Type = map.getValue(desc)

        fun top(a: Type, b: Type): Type {
            if (a == b) return a
            require(a.topType == b.topType)
            return a.topType.type
        }

        fun bottom(a: Type, b: Type): Type {
            if (a == b) return a
            require(a.topType == b.topType)
            if (a == a.topType.type) return b
            if (b == b.topType.type) return a
            throw IllegalArgumentException()
        }
    }
}

// O