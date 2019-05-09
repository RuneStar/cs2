package org.runestar.cs2

import org.runestar.cs2.util.forEachLine

interface Loader<T : Any> {

    fun load(id: Int): T?

    interface Keyed<T : Any> : Loader<T> {

        val ids: Set<Int>
    }

    data class Mapping<T : Any>(private val map: Map<Int, T>) : Keyed<T> {

        override fun load(id: Int): T? = map[id]

        override val ids: Set<Int> get() = map.keys
    }

    companion object {

        private fun readParamTypes(): Loader<Type> {
            val map = HashMap<Int, Type>()
            this::class.java.getResource("param-types.tsv").forEachLine { line ->
                val split = line.split('\t')
                val type = split[1].toInt()
                map[split[0].toInt()] = if (type == 0) Type.INT else Type.of(type)
            }
            return Mapping(map)
        }

        val PARAM_TYPES = readParamTypes()

        private fun readScriptNames(): Loader<ScriptName> {
            val map = HashMap<Int, ScriptName>()
            this::class.java.getResource("script-names.tsv").forEachLine { line ->
                val split = line.split('\t')
                map[split[0].toInt()] = ScriptName.of(split[1])
            }
            return Mapping(map)
        }

        val SCRIPT_NAMES = readScriptNames()

        private fun readNames(fileName: String): Loader<String> {
            val map = HashMap<Int, String>()
            this::class.java.getResource(fileName).forEachLine { line ->
                val split = line.split('\t')
                map[split[0].toInt()] = split[1]
            }
            return Mapping(map)
        }

        val GRAPHIC_NAMES = readNames("graphic-names.tsv")

        val STAT_NAMES = readNames("stat-names.tsv")

        val OBJ_NAMES = readNames("obj-names.tsv")

        val INV_NAMES = readNames("inv-names.tsv")

        val MAPAREA_NAMES = readNames("maparea-names.tsv")

        val PARAM_NAMES = readNames("param-names.tsv")
    }
}

fun <T : Any> Loader(load: (id: Int) -> T?) = object : Loader<T> {
    override fun load(id: Int): T? = load(id)
}

fun <T : Any> Loader<T>.caching(): Loader<T> = object : Loader<T> {
    private val cache = HashMap<Int, T?>()
    override fun load(id: Int): T? = cache[id] ?: if (id in cache) null else this@caching.load(id).also { cache[id] = it }
}

fun <T : Any> Loader<T>.withIds(keys: Set<Int>): Loader.Keyed<T> = object : Loader.Keyed<T>, Loader<T> by this {
    override val ids get() = keys
}