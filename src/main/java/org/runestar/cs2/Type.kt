package org.runestar.cs2

import org.runestar.cs2.util.CP1252
import org.runestar.cs2.util.toByte

enum class StackType {
    INT, STRING
}

class IncompatibleTypesException(val types: Collection<*>) : Exception("Incompatible types: $types") {
    constructor(vararg types: Any) : this(types.asList())
}

interface Type {

    val identifier: String

    val literal: String

    companion object {

        private val primitives = Primitive.values().associateBy { it.desc }

        fun of(desc: Byte): Primitive = primitives.getValue(desc)

        fun of(desc: Char): Primitive = of(desc.toByte(CP1252))

        fun ofAuto(desc: Byte): Primitive = if (desc == 0.toByte()) Primitive.INT else of(desc)

        fun union(types: Set<Type>): Type? {
            val size = types.size
            when (size) {
                0 -> return null
                1 -> return types.single()
            }
            // if (types.all { it is ArrayType }) return ArrayType(union(ListSet(types.mapTo(ArrayList(size)) { (it as ArrayType).elementType }))!!)
            if (size == 2) {
                if (Primitive.NAMEDOBJ in types && Primitive.OBJ in types) return Primitive.OBJ
                else if (Primitive.GRAPHIC in types && Primitive.FONTMETRICS in types) return Primitive.FONTMETRICS
                else if (Primitive.INT in types) types.firstOrNull { it is Alias }?.let { return it }
            }
            if (types.all { it == Primitive.INT || it is Alias }) return Primitive.INT
            throw IncompatibleTypesException(types)
        }

        fun intersection(types: Set<Type>): Type? {
            val size = types.size
            when (size) {
                0 -> return null
                1 -> return types.single()
            }
            // if (types.all { it is ArrayType }) return ArrayType(intersection(ListSet(types.mapTo(ArrayList(size)) { (it as ArrayType).elementType }))!!)
            if (size == 2) {
                if (Primitive.NAMEDOBJ in types && Primitive.OBJ in types) return Primitive.NAMEDOBJ
                else if (Primitive.GRAPHIC in types && Primitive.FONTMETRICS in types) return Primitive.GRAPHIC
                else if (Primitive.INT in types) types.firstOrNull { it is Alias }?.let { return it }
            }
            if (types.all { it == Primitive.INT || it is Alias }) return Primitive.INT
            throw IncompatibleTypesException(types)
        }
    }

    interface Stackable : Type {
        val stackType: StackType
    }
}

enum class Primitive(desc: Char) : Type.Stackable {

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
    ;

    val desc = desc.toByte(CP1252)

    override val stackType get() = if (this == STRING) StackType.STRING else StackType.INT

    override val identifier = name.toLowerCase()

    override val literal get() = identifier

    override fun toString() = identifier
}

enum class Alias : Type.Stackable {

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

    override val stackType get() = StackType.INT

    override val identifier = name.toLowerCase()

    override val literal get() = Primitive.INT.literal

    override fun toString() = identifier
}

data class ArrayType(val elementType: Type.Stackable) : Type {

    init {
        require(elementType.stackType == StackType.INT)
    }

    override val identifier get() = elementType.identifier + "array"

    override val literal get() = elementType.literal + "array"

    override fun toString() = identifier
}