package org.runestar.cs2

enum class TopType {
    INT, STRING
}

enum class Type(
        val desc: Char,
        val topType: TopType
) {
    INT('i', TopType.INT),
    STRING('s', TopType.STRING)

    // component, coord?, namedobj?, obj?, boolean?, enum?

    // descs: iIsdzoc1y`gmv
}