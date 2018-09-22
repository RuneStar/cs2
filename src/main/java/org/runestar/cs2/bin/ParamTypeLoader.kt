package org.runestar.cs2.bin

import org.runestar.cs2.Type

interface ParamTypeLoader {

    fun load(id: Int): Type

    object IntOnly : ParamTypeLoader {

        override fun load(id: Int) = Type.INT
    }
}