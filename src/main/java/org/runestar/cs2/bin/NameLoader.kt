package org.runestar.cs2.bin

interface NameLoader {

    fun load(id: Int): String?

    object None : NameLoader {

        override fun load(id: Int): String? = null
    }

    data class Mapping(private val map: Map<Int, String>): NameLoader {

        override fun load(id: Int): String? = map[id]
    }

    companion object {

        private fun readResource(fileName: String): NameLoader {
            val map = HashMap<Int, String>()
            NameLoader::class.java.getResource(fileName).openStream().bufferedReader().use { input ->
                input.lineSequence().forEach { line ->
                    val split = line.split('\t')
                    map[split[0].toInt()] = split[1]
                }
            }
            return Mapping(map)
        }

        val GRAPHICS = readResource("graphic-names.tsv")

        val FONTS = readResource("font-names.tsv")

        val SCRIPTS = readResource("script-names.tsv")

        val STATS = readResource("stat-names.tsv")
    }
}