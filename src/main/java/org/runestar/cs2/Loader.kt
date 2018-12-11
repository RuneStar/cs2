package org.runestar.cs2

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

interface Loader<T : Any> {

    fun load(id: Int): T?

    data class Mapping<T : Any>(private val map: Map<Int, T>) : Loader<T> {

        override fun load(id: Int): T? = map[id]
    }

    companion object {

        private fun readParamTypes(fileName: String): Loader<Type> {
            val map = HashMap<Int, Type>()
            Loader::class.java.getResource(fileName).openStream().bufferedReader().forEachLine { line ->
                val split = line.split('\t')
                val type = split[1].toInt()
                map[split[0].toInt()] = if (type == 0) Type.INT else Type.of(type)
            }
            return Mapping(map)
        }

        val PARAM_TYPES = readParamTypes("param-types.tsv")

        private fun readNames(fileName: String): Loader<String> {
            val map = HashMap<Int, String>()
            Loader::class.java.getResource(fileName).openStream().bufferedReader().forEachLine { line ->
                val split = line.split('\t')
                map[split[0].toInt()] = split[1]
            }
            return Mapping(map)
        }

        val GRAPHIC_NAMES = readNames("graphic-names.tsv")

        val SCRIPT_NAMES = readNames("script-names.tsv")

        val STAT_NAMES = readNames("stat-names.tsv")

        val OBJ_NAMES = readNames("obj-names.tsv")
    }

    class Scripts(val dir: Path) : Loader<Script> {

        override fun load(id: Int): Script {
            val file = dir.resolve(id.toString())
            val buffer = ByteBuffer.wrap(Files.readAllBytes(file))
            return Script.read(buffer)
        }
    }
}