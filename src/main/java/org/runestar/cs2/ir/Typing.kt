package org.runestar.cs2.ir

import org.runestar.cs2.ArrayType
import org.runestar.cs2.Primitive
import org.runestar.cs2.StackType
import org.runestar.cs2.Type
import org.runestar.cs2.util.ListSet

class Typing {

    var stackType: StackType? = null

    var isParameter = false

    private val from: MutableSet<Type> = ListSet(ArrayList(1))

    private val to: MutableSet<Type> = ListSet(ArrayList(1))

    var type: Type? = null
        private set

    val finalType: Type get() = type ?: if (stackType == StackType.STRING) Primitive.STRING else Primitive.INT

    fun from(t: Type?) = add(t, from)

    fun to(t: Type?) = add(t, to)

    private fun add(t: Type?, s: MutableSet<Type>): Boolean {
        if (t == null) return false
        if (t is Type.Stackable) stackType = t.stackType
        val b = s.add(t)
        if (b) type = calcType()
        return b
    }

    private fun calcType(): Type? {
        val u = Type.union(from)
        val i = Type.intersection(to)
        return if (u == null) i
        else if (i == null) u
        else if (u == i) u
        else Type.union(ListSet(mutableListOf(u, i)))
    }

    override fun toString() = "[$type:${from.joinToString("|")}:${to.joinToString("&")}]"

    companion object {

        fun from(t: Type?): Typing = Typing().also { it.from(t) }

        fun to(t: Type?): Typing = Typing().also { it.to(t) }

        fun compare(from: Typing, to: Typing): Boolean {
            val ft = from.type
            val tt = to.type
            if (ft == tt) return false
            return from.from(tt) or to.from(ft)
        }

        fun arrayGet(array: Typing, element: Typing): Boolean {
            val at = array.type as ArrayType?
            val et = element.type
            var b = false
            if (at != null) b = b or element.from(at.elementType)
            if (et != null) b = b or array.to(ArrayType(et as Type.Stackable))
            return b
        }

        fun arraySet(array: Typing, element: Typing): Boolean {
            val at = array.type as ArrayType?
            val et = element.type
            var b = false
            if (at != null) b = b or element.to(at.elementType)
            if (et != null) b = b or array.from(ArrayType(et as Type.Stackable))
            return b
        }

        fun returns(returns: Collection<List<Typing>>, signature: List<Typing>): Boolean {
            var b = false
            for (r in returns) {
                for (i in signature.indices) {
                    b = b or signature[i].from(r[i].type)
                }
            }
            for (r in returns) {
                for (i in signature.indices) {
                    b = b or r[i].from(signature[i].type)
                }
            }
            return b
        }
    }
}

class TypingFactory {

    private val globalVars = HashMap<VarId, Typing>()

    private val returns = HashMap<Int, List<Typing>>()

    inner class Local {

        private val vars = HashMap<VarId, Typing>()

        fun variable(varId: VarId): Typing {
            val v = if (varId.source.global) globalVars else vars
            return v.getOrPut(varId) { Typing() }
        }

        fun returned(scriptId: Int, size: Int): List<Typing> = returns.getOrPut(scriptId) { List(size) { Typing() } }
    }
}