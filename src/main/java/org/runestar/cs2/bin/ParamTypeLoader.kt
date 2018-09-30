package org.runestar.cs2.bin

import org.runestar.cs2.Type

interface ParamTypeLoader {

    fun load(id: Int): Type

    data class StringSet(val strings: Set<Int>) : ParamTypeLoader {

        override fun load(id: Int): Type = if (id in strings) Type.STRING else Type.INT
    }

    companion object {

        val DEFAULT: ParamTypeLoader = StringSet(setOf(451, 452, 453, 454, 455, 456, 457, 458, 506, 510, 559, 595))
    }
}