package org.runestar.cs2.ir

import org.runestar.cs2.bin.StackType
import org.runestar.cs2.bin.Type

class Typing(val stackType: StackType) {

    var _type: Type? = null

    var _identifier: String? = null

    var freezeType = false

    var freezeIdentifier = false

    val from = HashSet<Typing>()

    val to = HashSet<Typing>()

    val compare = HashSet<Typing>()
}

val Typing.type get() = checkNotNull(_type)

val Typing.literal get() = type.literal

val Typing.identifier get() = checkNotNull(_identifier)

val Typing.prototype get() = Prototype(type, identifier)

val Typing._prototype: Prototype? get() {
    val t = _type ?: return null
    val i = _identifier ?: return null
    return Prototype(t, i)
}

fun Typing.freeze(type: Type) {
    freezeType = true
    check(type.stackType == stackType)
    _type = type
}

fun Typing.freeze(identifier: String) {
    freezeIdentifier = true
    _identifier = identifier
}

fun Typing.freeze(prototype: Prototype) {
    freeze(prototype.type)
    freeze(prototype.identifier)
}

fun Typing(prototype: Prototype) = Typing(prototype.stackType).apply { freeze(prototype) }

fun assign(from: Typing, to: Typing) {
    require(from != to)
    require(from.stackType == to.stackType)
    from.to.add(to)
    to.from.add(from)
}

fun assign(from: List<Typing>, to: List<Typing>) {
    require(from.size == to.size)
    for (i in from.indices) {
        assign(from[i], to[i])
    }
}

fun compare(a: Typing, b: Typing) {
    require(a != b)
    require(a.stackType == b.stackType)
    a.compare.add(b)
    b.compare.add(a)
}

fun remove(t: Typing) {
    t.from.forEach { it.to.remove(t) }
    t.to.forEach { it.from.remove(t) }
    t.compare.forEach { it.compare.remove(t) }
}

fun replace(t: Typing, by: Typing) {
    require(t != by)
    require(t.stackType == by.stackType)
    check(t._type == null && t._identifier == null)
    t.from.forEach {
        it.to.remove(t)
        if (by != it) {
            it.to.add(by)
            by.from.add(it)
        }
    }
    t.to.forEach {
        it.from.remove(t)
        if (by != it) {
            it.from.add(by)
            by.to.add(it)
        }
    }
    t.compare.forEach {
        it.compare.remove(t)
        if (by != it) {
            it.compare.add(by)
            by.compare.add(it)
        }
    }
}

fun replace(t: List<Typing>, by: List<Typing>) {
    require(t.size == by.size)
    for (i in t.indices) {
        replace(t[i], by[i])
    }
}