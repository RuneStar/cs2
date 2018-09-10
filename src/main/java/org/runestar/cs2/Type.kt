package org.runestar.cs2

enum class TopType {
    INT, STRING
}

enum class Type(
        val desc: Char,
        val topType: TopType = TopType.INT
) {
    INT('i'),
    STRING('s', TopType.STRING),
    COMPONENT('I'),
    BOOLEAN('1'),
    OBJ('o'),
    ENUM('g')
}