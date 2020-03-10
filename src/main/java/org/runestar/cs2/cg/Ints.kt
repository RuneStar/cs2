package org.runestar.cs2.cg

import org.runestar.cs2.Alias.*
import org.runestar.cs2.ArrayType
import org.runestar.cs2.Loader
import org.runestar.cs2.Primitive.*
import org.runestar.cs2.Type
import org.runestar.cs2.loadNotNull
import org.runestar.cs2.map
import org.runestar.cs2.mapIndexed
import org.runestar.cs2.orElse

private val VALUE = Loader { it.toString() }

private val NULL = Loader { if (it == -1) "null" else null }

private fun Loader<String>.quote() = map { '"' + it + '"' }

private fun Loader<String>.prefix(prefix: String) = map { prefix + it }

private val COORDS = Loader {
    val plane = it ushr 28
    val x = (it ushr 14) and 0x3FFF
    val z = it and 0x3FFF
    "${plane}_${x / 64}_${z / 64}_${x and 0x3F}_${z and 0x3F}"
}

private val COLOUR_CONSTANTS = Loader.Mapping(mapOf(
        0xFF0000 to "^red",
        0x00FF00 to "^green",
        0x0000FF to "^blue",
        0xFFFF00 to "^yellow",
        0xFF00FF to "^magenta",
        0x00FFFF to "^cyan",
        0xFFFFFF to "^white",
        0x000000 to "^black"
))

private val COLOURS = Loader {
    check((it shr 24) == 0)
    "0x" + it.toString(16).padStart(6, '0')
}

private val INT_CONSTANTS = Loader.Mapping(mapOf(
        Int.MAX_VALUE to "^max_32bit_int",
        Int.MIN_VALUE to "^min_32bit_int"
))

private val INTERFACES = unique(INTERFACE, Loader.INTERFACE_NAMES)

private val COMPONENTS = Loader { INTERFACES.loadNotNull(it shr 16) + ':' + (it and 0xFFFF) }

private fun cst(prefix: String, loader: Loader<String>) = loader.prefix('^' + prefix + '_').orElse(NULL)

private fun Loader<String>.idSuffix() = mapIndexed { id, n -> n + '_' + id }

private fun unique(type: Type, loader: Loader<String>) = loader.orElse(unknown(type))

private fun uniqueExhaustive(loader: Loader<String>) = loader.orElse(NULL)

private fun unknown(type: Type) = NULL.orElse(Loader(type.identifier).idSuffix())

private fun nonUnique(type: Type, loader: Loader<String>) = NULL.orElse(loader.orElse(Loader(type.identifier)).idSuffix())

private val TYPES = HashMap<Type.Stackable, Loader<String>>().apply {
    this[INT] = INT_CONSTANTS.orElse(VALUE)
    this[COORD] = NULL.orElse(COORDS)
    this[COLOUR] = NULL.orElse(COLOUR_CONSTANTS).orElse(COLOURS)
    this[COMPONENT] = NULL.orElse(COMPONENTS)
    this[TYPE] = Loader { Type.of(it.toByte()).identifier }
    this[BIT] = Loader.BOOLEAN_NAMES.prefix("^").orElse(NULL)
    this[VAR] = VALUE.prefix("var")
    this[GRAPHIC] = NULL.orElse(Loader.GRAPHIC_NAMES.orElse(Loader(GRAPHIC.identifier).idSuffix()).quote())

    this[ENUM] = unknown(ENUM)
    this[CATEGORY] = unknown(CATEGORY)

    this[CHAR] = NULL
    this[AREA] = NULL
    this[MAPELEMENT] = NULL

    this[BOOLEAN] = uniqueExhaustive(Loader.BOOLEAN_NAMES)
    this[STAT] = uniqueExhaustive(Loader.STAT_NAMES)
    this[MAPAREA] = uniqueExhaustive(Loader.MAPAREA_NAMES)

    this[FONTMETRICS] = unique(FONTMETRICS, Loader.GRAPHIC_NAMES)
    this[INV] = unique(INV, Loader.INV_NAMES)
    this[SYNTH] = unique(SYNTH, Loader.SYNTH_NAMES)
    this[PARAM] = unique(PARAM, Loader.PARAM_NAMES)
    this[INTERFACE] = INTERFACES

    this[OBJ] = nonUnique(OBJ, Loader.OBJ_NAMES)
    this[LOC] = nonUnique(LOC, Loader.LOC_NAMES)
    this[MODEL] = nonUnique(MODEL, Loader.MODEL_NAMES)
    this[STRUCT] = nonUnique(STRUCT, Loader.STRUCT_NAMES)
    this[NPC] = nonUnique(NPC, Loader.NPC_NAMES)
    this[SEQ] = nonUnique(SEQ, Loader.SEQ_NAMES)
    this[NAMEDOBJ] = getValue(OBJ)

    this[KEY] = cst("key", Loader.KEY_NAMES)
    this[IFTYPE] = cst("iftype", Loader.IFTYPE_NAMES)
    this[SETSIZE] = cst("setsize", Loader.SETSIZE_NAMES)
    this[SETPOSH] = cst("setpos", Loader.SETPOSH_NAMES)
    this[SETPOSV] = cst("setpos", Loader.SETPOSV_NAMES)
    this[SETTEXTALIGNH] = cst("settextalign", Loader.SETTEXTALIGNH_NAMES)
    this[SETTEXTALIGNV] = cst("settextalign", Loader.SETTEXTALIGNV_NAMES)
    this[CHATTYPE] = cst("chattype", Loader.CHATTYPE_NAMES)
    this[WINDOWMODE] = cst("windowmode", Loader.WINDOWMODE_NAMES)
    this[CLIENTTYPE] = cst("clienttype", Loader.CLIENTTYPE_NAMES)
}

fun intValueToString(n: Int, type: Type): String {
    return if (type is ArrayType) {
        type.identifier + n
    } else {
        TYPES.getValue(type as Type.Stackable).loadNotNull(n)
    }
}