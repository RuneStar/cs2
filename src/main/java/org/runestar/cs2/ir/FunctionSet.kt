package org.runestar.cs2.ir

class FunctionSet(
        val functions: Map<Int, Function>,
        val typings: Typings,
        val callGraph: CallGraph,
)