package org.runestar.cs2.bin

import org.runestar.cs2.Type

interface ParamTypeLoader {

    fun load(id: Int): Type

    data class StringSet(val strings: Set<Int>) : ParamTypeLoader {

        override fun load(id: Int): Type = (if (id in strings) Type.STRING else Type.INT)
    }
}