package org.runestar.cs2.bin

import org.runestar.cs2.Type

interface ParamTypeLoader {

    fun load(id: Int): Type

    data class Mapping(private val map: Map<Int, Type>): ParamTypeLoader {

        override fun load(id: Int): Type = map.getValue(id)
    }

    companion object {

        private fun readResource(fileName: String): ParamTypeLoader {
            val map = HashMap<Int, Type>()
            ParamTypeLoader::class.java.getResource(fileName).openStream().bufferedReader().use { input ->
                input.lineSequence().forEach { line ->
                    val split = line.split('\t')
                    val id = split[0].toInt()
                    val type = split[1].toInt()
                    map[id] = if (type == 0) Type.INT else Type.of(type)
                }
            }
            return Mapping(map)
        }

        val DEFAULT: ParamTypeLoader = readResource("param-types.tsv")
    }
}