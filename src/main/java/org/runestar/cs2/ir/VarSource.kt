package org.runestar.cs2.ir

enum class VarSource(val global: Boolean, val local: Boolean) {

    STACK(false, false),
    LOCALINT(false, true),
    LOCALSTRING(false, true),
    ARRAY(false, false),
    VARP(true, false),
    VARBIT(true, false),
    VARCINT(true, false),
    VARCSTRING(true, false),
    ;
}

data class VarId(val source: VarSource, val id: Int)