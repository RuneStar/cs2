package org.runestar.cs2.dfa

import org.runestar.cs2.bin.StackType
import org.runestar.cs2.bin.Type
import org.runestar.cs2.bin.Type.*
import org.runestar.cs2.ir.FunctionSet
import org.runestar.cs2.ir.Typing
import org.runestar.cs2.ir.freeze
import org.runestar.cs2.ir.type

class CalcTypes(private val fs: FunctionSet) {

    companion object : Phase {
        override fun transform(fs: FunctionSet) = CalcTypes(fs).transform()
    }

    private val all = fs.typings.all()

    private val unidentified = all.filterTo(LinkedHashSet()) { it._type == null }

    private fun transform() {
        strings()
        while (down());
        while (namedObjs() || any() || compare());
        // todo
        while (check());
        defaults()
    }

    private fun strings() {
        val itr = unidentified.iterator()
        for (t in itr) {
            if (t.stackType == StackType.STRING) {
                t.freeze(STRING)
                itr.remove()
            }
        }
    }

    private fun defaults() {
        for (t in unidentified) {
            t._type = t.stackType.defaultType
        }
        unidentified.clear()
    }

    private fun namedObjs(): Boolean {
        var b = false
        for (t in fs.typings.constants.values) {
            if (t._type == OBJ) {
                b = true
                t.freeze(NAMEDOBJ)
            }
        }
        return b
    }

    private fun down(): Boolean {
        val u = HashSet<Type>()
        var b = false
        val itr = unidentified.iterator()
        out@
        for (t in itr) {
            u.clear()
            for (f in t.from) {
                if (f._type == null) continue@out
                u.add(f.type)
            }
            if (u.isNotEmpty()) {
                b = true
                t.freeze(Type.union(u)!!)
                itr.remove()
            }
        }
        return b
    }

    private fun any(): Boolean {
        val newTypes = HashMap<Typing, Type>()
        val u = HashSet<Type>()
        val i = HashSet<Type>()
        val x = HashSet<Type>()
        var b = false
        val itr = unidentified.iterator()
        for (t in itr) {
            u.clear()
            i.clear()
            t.from.mapNotNullTo(u) { it._type }
            t.to.mapNotNullTo(i) { it._type }
            if (u.isNotEmpty() || i.isNotEmpty()) {
                x.clear()
                Type.union(u)?.let { x.add(it) }
                Type.intersection(i)?.let { x.add(it) }
                newTypes[t] = Type.intersection(x)!!
                b = true
                itr.remove()
            }
        }
        for ((t, y) in newTypes) {
            t._type = y
        }
        return b
    }

    private fun check(): Boolean {
        val newTypes = HashMap<Typing, Type>()
        val u = HashSet<Type>()
        var b = false
        for (t in all) {
            if (t._type == null || t.freezeType) continue
            u.clear()
            t.from.mapNotNullTo(u) { it._type }
            if (u.isNotEmpty()) {
                val newType = Type.union(u)!!
                if (t._type != newType) {
                    b = true
                    newTypes[t] = newType
                }
            }
        }
        for ((t, y) in newTypes) {
            t._type = y
        }
        return b
    }

    private fun compare(): Boolean {
        val newTypes = HashMap<Typing, Type>()
        val u = HashSet<Type>()
        var b = false
        val itr = unidentified.iterator()
        for (t in itr) {
            u.clear()
            t.compare.mapNotNullTo(u) { it._type }
            if (u.isNotEmpty()) {
                newTypes[t] = Type.union(u)!!
                b = true
                itr.remove()
            }
        }
        for ((t, y) in newTypes) {
            t._type = y
        }
        return b
    }
}