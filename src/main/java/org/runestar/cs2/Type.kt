package org.runestar.cs2

import java.lang.AssertionError

interface Type {

    val nameLiteral: String

    val typeLiteral: String

    fun erase(): Type

    companion object {

        private val map = Primitive.values().filter { it.desc != null }.associateBy { it.desc }

        fun of(desc: Char): Primitive = map.getValue(desc)

        fun of(desc: Int): Primitive = of(desc.toChar())

        fun merge(a: Type, b: Type): Type {
            if (a == b) return a
            if (a.javaClass != b.javaClass) {
                if (a == Primitive.INT && b is ArrayType) return b
                if (b == Primitive.INT && a is ArrayType) return a
                incompatibleTypes(a, b)
            }
            return when (a) {
                is Primitive -> merge(a, b as Primitive)
                is ArrayType -> ArrayType(merge(a.elementType, (b as ArrayType).elementType))
                else -> throw AssertionError()
            }
        }

        private fun merge(a: Primitive, b: Primitive): Primitive {
            if (a == b) return a
            if (a.erase() != b.erase()) incompatibleTypes(a, b)
            if (a == Primitive.INT) return b
            if (b == Primitive.INT) return a
            if ((a == Primitive.OBJ || a == Primitive.NAMEDOBJ) && (b == Primitive.OBJ || b == Primitive.NAMEDOBJ)) return Primitive.OBJ
            if ((a == Primitive.FONTMETRICS || a == Primitive.GRAPHIC) && (b == Primitive.FONTMETRICS || b == Primitive.GRAPHIC)) return Primitive.FONTMETRICS
            incompatibleTypes(a, b)
        }

        fun merge(a: List<Type>, b: List<Type>): List<Type> {
            if (a.size != b.size) incompatibleTypes(a, b)
            return List(a.size) { merge(a[it], b[it]) }
        }

        private fun <T> incompatibleTypes(a: T, b: T): Nothing = throw IllegalArgumentException("Incompatible types: $a & $b")
    }
}

enum class Primitive(val desc: Char? = null) : Type {

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

    MAPELEMENT('µ'),
    NPC('n'),
    SEQ('A'),

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
    WINDOWMODE,
    KEY,
    ;

    override val nameLiteral: String = name.toLowerCase()

    override val typeLiteral: String get() = if (desc == null) INT.nameLiteral else nameLiteral

    override fun erase(): Primitive = if (this == STRING) STRING else INT

    override fun toString() = nameLiteral
}

data class ArrayType(val elementType: Primitive) : Type {

    init {
        require(elementType != Primitive.STRING)
    }

    override val nameLiteral get() = elementType.nameLiteral + "array"

    override val typeLiteral get() = elementType.typeLiteral + "array"

    override fun erase(): ArrayType = INT

    override fun toString() = nameLiteral

    companion object {
        val INT = ArrayType(Primitive.INT)
    }
}