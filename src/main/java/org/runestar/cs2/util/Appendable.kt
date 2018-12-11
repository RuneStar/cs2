package org.runestar.cs2.util

import java.lang.Appendable

fun <T : Appendable> T.append(n: Int): T = apply { append(n.toString()) }

fun <T : Appendable> T.append(b: Boolean): T = apply { append(b.toString()) }