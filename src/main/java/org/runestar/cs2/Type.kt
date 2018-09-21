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
    INV('v');

    val literal = name.toLowerCase()

    val topType: TopType get() = if (this == STRING) TopType.STRING else TopType.INT

    companion object {

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

// O, R, `, c, l, m, y