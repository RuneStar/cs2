package org.runestar.cs2.ir

import org.runestar.cs2.bin.Type

data class Prototype(
        val type: Type,
        val identifier: String,
) {

    constructor(type: Type) : this(type, type.literal)

    override fun toString() = type.literal + '-' + identifier
}

val Prototype.literal get() = type.literal

val Prototype.isDefault get() = identifier == literal

val Prototype.stackType get() = type.stackType

val INT = Prototype(Type.INT)
val STRING = Prototype(Type.STRING)
val COMPONENT = Prototype(Type.COMPONENT)
val BOOLEAN = Prototype(Type.BOOLEAN)
val OBJ = Prototype(Type.OBJ)
val ENUM = Prototype(Type.ENUM)
val STAT = Prototype(Type.STAT)
val GRAPHIC = Prototype(Type.GRAPHIC)
val INV = Prototype(Type.INV)
val MODEL = Prototype(Type.MODEL)
val COORD = Prototype(Type.COORD)
val CATEGORY = Prototype(Type.CATEGORY)
val LOC = Prototype(Type.LOC)
val AREA = Prototype(Type.AREA)
val MAPAREA = Prototype(Type.MAPAREA)
val NAMEDOBJ = Prototype(Type.NAMEDOBJ)
val FONTMETRICS = Prototype(Type.FONTMETRICS)
val CHAR = Prototype(Type.CHAR)
val STRUCT = Prototype(Type.STRUCT)
val SYNTH = Prototype(Type.SYNTH)
val MAPELEMENT = Prototype(Type.MAPELEMENT)
val NPC = Prototype(Type.NPC)
val SEQ = Prototype(Type.SEQ)
val INTERFACE = Prototype(Type.INTERFACE)
val TYPE = Prototype(Type.TYPE)
val PARAM = Prototype(Type.PARAM)
val NEWVAR = Prototype(Type.NEWVAR)
val NPC_UID = Prototype(Type.NPC_UID)
val PLAYER_UID = Prototype(Type.PLAYER_UID)

val OPBASE = Prototype(Type.STRING, "opbase")
val MOUSEX = Prototype(Type.INT, "mousex")
val MOUSEY = Prototype(Type.INT, "mousey")
// com
val OPINDEX = Prototype(Type.INT, "opindex")
val COMSUBID = Prototype(Type.INT, "comsubid")
val DROP = Prototype(Type.COMPONENT, "drop")
val DROPSUBID = Prototype(Type.INT, "dropsubid")
val KEY = Prototype(Type.INT, "key")
val KEYCHAR = Prototype(Type.CHAR, "keychar")

val INDEX = Prototype(Type.INT, "index")
val LENGTH = Prototype(Type.INT, "length")
val COUNT = Prototype(Type.INT, "count")
val TOTAL = Prototype(Type.INT, "total")
val SIZE = Prototype(Type.INT, "size")
val NUM = Prototype(Type.INT, "num")
val X = Prototype(Type.INT, "x")
val Y = Prototype(Type.INT, "y")
val Z = Prototype(Type.INT, "z")
val WIDTH = Prototype(Type.INT, "width")
val HEIGHT = Prototype(Type.INT, "height")
val WORLD = Prototype(Type.INT, "world")
val RANK = Prototype(Type.INT, "rank")
val XP = Prototype(Type.INT, "xp")
val LVL = Prototype(Type.INT, "lvl")
val SLOT = Prototype(Type.INT, "slot")
val CLANSLOT = Prototype(Type.INT, "clanslot")
val CLOCK = Prototype(Type.INT, "clock")
val TRANS = Prototype(Type.INT, "trans")
val ANGLE = Prototype(Type.INT, "angle")
val CHATFILTER = Prototype(Type.INT, "chatfilter")
val MESUID = Prototype(Type.INT, "mesuid")
val FLAGS = Prototype(Type.INT, "flags")
val LAYER = Prototype(Type.COMPONENT, "layer")
val OP = Prototype(Type.STRING, "op")
val URL = Prototype(Type.STRING, "url")
val TEXT = Prototype(Type.STRING, "text")
val MES = Prototype(Type.STRING, "mes")
val USERNAME = Prototype(Type.STRING, "username")

val COLOUR = Prototype(Type.INT, "colour")
val IFTYPE = Prototype(Type.INT, "iftype")
val SETSIZE = Prototype(Type.INT, "setsize")
val SETPOSH = Prototype(Type.INT, "setposh")
val SETPOSV = Prototype(Type.INT, "setposv")
val SETTEXTALIGNH = Prototype(Type.INT, "settextalignh")
val SETTEXTALIGNV = Prototype(Type.INT, "settextalignv")
val CHATTYPE = Prototype(Type.INT, "chattype")
val BOOL = Prototype(Type.INT, "bool")
val WINDOWMODE = Prototype(Type.INT, "windowmode")
val CLIENTTYPE = Prototype(Type.INT, "clienttype")
