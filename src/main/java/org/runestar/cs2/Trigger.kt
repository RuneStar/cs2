package org.runestar.cs2

enum class Trigger(val id: Int) {

    opworldmapelement1(10),
    opworldmapelement2(11),
    opworldmapelement3(12),
    opworldmapelement4(13),
    opworldmapelement5(14),
    worldmapelementmouseover(15),
    worldmapelementmouseleave(16),
    worldmapelementmouserepeat(17),
    proc(73),
    clientscript(76),
    ;

    companion object {

        private val map = values().associateBy { it.id }

        fun of(id: Int): Trigger = map.getValue(id)
    }
}