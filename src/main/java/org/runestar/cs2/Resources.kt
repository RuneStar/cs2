package org.runestar.cs2

import org.runestar.cs2.bin.ScriptName
import org.runestar.cs2.bin.Type
import org.runestar.cs2.util.Loader
import org.runestar.cs2.util.thisClass

private fun <T : Any> readLoader(fileName: String, valueMapper: (String) -> T): Loader.Map<T> {
    val map = HashMap<Int, T>()
    thisClass.getResourceAsStream(fileName).bufferedReader().use {
        while (true) {
            val line = it.readLine() ?: break
            val tab = line.indexOf('\t')
            val id = line.substring(0, tab).toInt()
            val v = valueMapper(line.substring(tab + 1))
            check(map.put(id, v) == null)
        }
    }
    return Loader(map)
}

private fun readNames(fileName: String): Loader.Map<String> = readLoader(fileName) { it }

val PARAM_TYPES = readLoader("param-types.tsv") { Type.ofAuto(it.toByte()) }

val SCRIPT_NAMES = readLoader("script-names.tsv") { ScriptName(it) }

val BOOLEAN_NAMES = readNames("boolean-names.tsv")
val FONTMETRICS_NAMES = readNames("fontmetrics-names.tsv")
val GRAPHIC_NAMES = readNames("graphic-names.tsv")
val INTERFACE_NAMES = readNames("interface-names.tsv")
val INV_NAMES = readNames("inv-names.tsv")
val LOC_NAMES = readNames("loc-names.tsv")
val MAPAREA_NAMES = readNames("maparea-names.tsv")
val MODEL_NAMES = readNames("model-names.tsv")
val NPC_NAMES = readNames("npc-names.tsv")
val OBJ_NAMES = readNames("obj-names.tsv")
val PARAM_NAMES = readNames("param-names.tsv")
val SEQ_NAMES = readNames("seq-names.tsv")
val STAT_NAMES = readNames("stat-names.tsv")
val STRUCT_NAMES = readNames("struct-names.tsv")
val SYNTH_NAMES = readNames("synth-names.tsv")

val CHATFILTER_NAMES = readNames("chatfilter-names.tsv")
val CHATTYPE_NAMES = readNames("chattype-names.tsv")
val CLIENTTYPE_NAMES = readNames("clienttype-names.tsv")
val IFTYPE_NAMES = readNames("iftype-names.tsv")
val KEY_NAMES = readNames("key-names.tsv")
val SETPOSH_NAMES = readNames("setposh-names.tsv")
val SETPOSV_NAMES = readNames("setposv-names.tsv")
val SETSIZE_NAMES = readNames("setsize-names.tsv")
val SETTEXTALIGNH_NAMES = readNames("settextalignh-names.tsv")
val SETTEXTALIGNV_NAMES = readNames("settextalignv-names.tsv")
val WINDOWMODE_NAMES = readNames("windowmode-names.tsv")