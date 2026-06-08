package com.mangamojo.app.providers.common

class TimedMemoryCache<K, V>(
    private val ttlMillis: Long,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val entries = mutableMapOf<K, Entry<V>>()

    fun get(key: K): V? {
        val entry = synchronized(entries) { entries[key] } ?: return null
        if (now() - entry.cachedAt > ttlMillis) {
            synchronized(entries) { entries.remove(key) }
            return null
        }
        return entry.value
    }

    fun put(key: K, value: V): V {
        synchronized(entries) { entries[key] = Entry(value, now()) }
        return value
    }

    fun clear() {
        synchronized(entries) { entries.clear() }
    }

    private data class Entry<V>(
        val value: V,
        val cachedAt: Long,
    )
}
