package org.runestar.cs2.util

fun interface Loader<T : Any> {

    fun load(id: Int): T?

    interface Keyed<T : Any> : Loader<T> {

        val ids: Set<Int>
    }

    data class Map<T : Any>(val map: kotlin.collections.Map<Int, T>) : Keyed<T> {

        override fun load(id: Int): T? = map[id]

        override val ids: Set<Int> get() = map.keys
    }

    data class Constant<T : Any>(val t: T): Loader<T> {

        override fun load(id: Int) = t
    }
}

fun <T : Any> Loader<T>.caching(): Loader<T> = object : Loader<T> {

    private val cache = HashMap<Int, T?>()

    override fun load(id: Int): T? = cache[id] ?: if (id in cache) null else this@caching.load(id).also { cache[id] = it }
}

fun <T : Any> Loader<T>.withIds(ids: Set<Int>): Loader.Keyed<T> = object : Loader.Keyed<T>, Loader<T> by this {

    override val ids get() = ids
}

fun <T : Any> Loader<T>.loadNotNull(id: Int): T = checkNotNull(load(id)) { "Value for id $id was null" }

fun <T : Any> Loader<T>.orElse(other: Loader<T>): Loader<T> = Loader { load(it) ?: other.load(it) }

fun <T : Any, E : Any> Loader<T>.map(transform: (T) -> E): Loader<E> = Loader { load(it)?.let(transform) }

fun <T : Any, E : Any> Loader<T>.mapIndexed(transform: (Int, T) -> E): Loader<E> = Loader { id -> load(id)?.let { transform(id, it) } }

fun <T : Any> Loader(t: T) = Loader.Constant(t)

fun <T : Any> Loader(map: Map<Int, T>) = Loader.Map(map)