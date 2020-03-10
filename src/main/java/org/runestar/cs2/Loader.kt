package org.runestar.cs2

interface Loader<T : Any> {

    fun load(id: Int): T?

    interface Keyed<T : Any> : Loader<T> {

        val ids: Set<Int>
    }

    data class Mapping<T : Any>(val map: Map<Int, T>) : Keyed<T> {

        override fun load(id: Int): T? = map[id]

        override val ids: Set<Int> get() = map.keys
    }

    companion object {

        private fun <T : Any> readLoader(fileName: String, valueMapper: (String) -> T): Loader<T> {
            val map = HashMap<Int, T>()
            this::class.java.getResource(fileName).openStream().bufferedReader().use {
                while (true) {
                    val line = it.readLine() ?: break
                    val tab = line.indexOf('\t')
                    map[line.substring(0, tab).toInt()] = valueMapper(line.substring(tab + 1))
                }
            }
            return Mapping(map)
        }

        private fun readNames(fileName: String): Loader<String> = readLoader(fileName) { it }

        val PARAM_TYPES = readLoader("param-types.tsv") { Type.ofAuto(it.toByte()) }

        val SCRIPT_NAMES = readLoader("script-names.tsv") { ScriptName.of(it) }

        val GRAPHIC_NAMES = readNames("graphic-names.tsv")

        val STAT_NAMES = readNames("stat-names.tsv")

        val OBJ_NAMES = readNames("obj-names.tsv")

        val INV_NAMES = readNames("inv-names.tsv")

        val MAPAREA_NAMES = readNames("maparea-names.tsv")

        val PARAM_NAMES = readNames("param-names.tsv")

        val KEY_NAMES = readNames("key-names.tsv")

        val CHATTYPE_NAMES = readNames("chattype-names.tsv")

        val LOC_NAMES = readNames("loc-names.tsv")

        val MODEL_NAMES = readNames("model-names.tsv")

        val STRUCT_NAMES = readNames("struct-names.tsv")

        val NPC_NAMES = readNames("npc-names.tsv")

        val SEQ_NAMES = readNames("seq-names.tsv")

        val BOOLEAN_NAMES = readNames("boolean-names.tsv")

        val WINDOWMODE_NAMES = readNames("windowmode-names.tsv")

        val IFTYPE_NAMES = readNames("iftype-names.tsv")

        val SETSIZE_NAMES = readNames("setsize-names.tsv")

        val SETPOSH_NAMES = readNames("setposh-names.tsv")

        val SETPOSV_NAMES = readNames("setposv-names.tsv")

        val SETTEXTALIGNH_NAMES = readNames("settextalignh-names.tsv")

        val SETTEXTALIGNV_NAMES = readNames("settextalignv-names.tsv")

        val CLIENTTYPE_NAMES = readNames("clienttype-names.tsv")

        val INTERFACE_NAMES = readNames("interface-names.tsv")

        val SYNTH_NAMES = readNames("synth-names.tsv")
    }
}

fun <T : Any> Loader(load: (id: Int) -> T?) = object : Loader<T> {
    override fun load(id: Int): T? = load(id)
}

fun <T : Any> Loader<T>.caching(): Loader<T> = object : Loader<T> {
    private val cache = HashMap<Int, T?>()
    override fun load(id: Int): T? = cache[id] ?: if (id in cache) null else this@caching.load(id).also { cache[id] = it }
}

fun <T : Any> Loader<T>.withIds(ids: Set<Int>): Loader.Keyed<T> = object : Loader.Keyed<T>, Loader<T> by this {
    override val ids get() = ids
}

fun <T : Any> Loader<T>.loadNotNull(id: Int): T = checkNotNull(load(id)) { "Value for id $id was null" }

fun <T : Any> Loader<T>.orElse(other: Loader<T>): Loader<T> = Loader { load(it) ?: other.load(it) }

fun <T : Any, E : Any> Loader<T>.map(transform: (T) -> E): Loader<E> = Loader { load(it)?.let(transform) }

fun <T : Any, E : Any> Loader<T>.mapIndexed(transform: (Int, T) -> E): Loader<E> = Loader { id -> load(id)?.let { transform(id, it) } }

fun <T : Any> Loader(t: T) = Loader { t }