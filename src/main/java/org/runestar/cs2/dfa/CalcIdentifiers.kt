package org.runestar.cs2.dfa

import org.runestar.cs2.ir.*

class CalcIdentifiers(private val fs: FunctionSet) {

    companion object : Phase {
        override fun transform(fs: FunctionSet) = CalcIdentifiers(fs).transform()

        private val STRONG = setOf(MESUID, CHATFILTER, RANK, WORLD, KEY, IFTYPE, SETSIZE, SETPOSH, SETPOSV, SETTEXTALIGNH, SETTEXTALIGNV, CHATTYPE, WINDOWMODE, CLIENTTYPE)
    }

    private val unidentified = fs.typings.all().filterTo(LinkedHashSet()) { it._identifier == null }

    private fun transform() {
        while (
                flow(::safeSingle) { from } ||
                flow(::safeSingle) { to }
        );
        constantComparisons()
        // todo
        while (
                flow(::union) { from } ||
                flow(::union) { to } ||
                flow(::union) { compare }
        );
        defaults()
    }

    private fun flow(
            merge: (MutableSet<Prototype?>) -> Prototype?,
            access: Typing.() -> Set<Typing>,
    ): Boolean {
        val s = HashSet<Prototype?>()
        var b = false
        val itr = unidentified.iterator()
        for (t in itr) {
            val f = t.access()
            if (f.isEmpty()) continue
            s.clear()
            f.mapTo(s) { it._prototype }
            val p = merge(s) ?: continue
            b = true
            check(t._type == p.type)
            t.freeze(p.identifier)
            itr.remove()
        }
        return b
    }

    private fun constantComparisons(): Boolean {
        var b = false
        for (t in fs.typings.constants.values) {
            if (t._identifier != null) continue
            check(t.from.isEmpty())
            if (t.compare.isNotEmpty()) {
                check(t.to.isEmpty())
                val c = t.compare.single()
                if (c._identifier != null) {
                    t.freeze(c.identifier)
                    b = true
                    unidentified.remove(t)
                }
            }
        }
        return b
    }

    private fun defaults() {
        for (t in unidentified) {
            t._identifier = t.type.literal
        }
        unidentified.clear()
    }

    private fun safeSingle(s: MutableSet<Prototype?>): Prototype? {
        val p = s.singleOrNull() ?: return null
        if (p.isDefault) return null
        return p
    }

    private fun union(s: MutableSet<Prototype?>): Prototype? {
        for (p in s) {
            if (p != null && p in STRONG) return p
        }
        if (null in s) return null
        s as MutableSet<Prototype>
        if (s.any { it.isDefault }) return null
        when (s.size) {
            1 -> return s.iterator().next()
        }
        if (X in s && WIDTH in s) s.remove(WIDTH)
        if (Y in s && HEIGHT in s) s.remove(HEIGHT)
        if (X in s && MOUSEX in s) s.remove(MOUSEX)
        if (Y in s && MOUSEY in s) s.remove(MOUSEY)
        if (SLOT in s && COMSUBID in s) s.remove(COMSUBID)
        if (SLOT in s && DROPSUBID in s) s.remove(DROPSUBID)
        if (COMSUBID in s && DROPSUBID in s) s.remove(DROPSUBID)
        if (INDEX in s && COMSUBID in s) s.remove(COMSUBID)
        if (INDEX in s && COUNT in s) s.remove(COUNT)
        if (COUNT in s && LENGTH in s) s.remove(LENGTH)
        if (COMSUBID in s && COUNT in s) s.remove(COUNT)
        if (OP in s && TEXT in s) s.remove(TEXT)
        return s.singleOrNull()
    }
}
