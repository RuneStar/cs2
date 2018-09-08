package org.runestar.cs2.ir1

interface Arg {

    interface Const : Arg {

        class Num(val cst: Int) : Const {

            override fun toString(): String = cst.toString()
        }

        class Str(val cst: String?) : Const {

            override fun toString(): String = cst.toString()
        }
    }

    sealed class Var(val tag: Char, val index: Int) : Arg {

        override fun toString(): String = "$$tag$index"

        interface Num

        interface Str

        class IS(index: Int) : Var('i', index), Num

        class IL(index: Int) : Var('j', index), Num

        class SS(index: Int) : Var('s', index), Str

        class SL(index: Int) : Var('z', index), Str
    }
}