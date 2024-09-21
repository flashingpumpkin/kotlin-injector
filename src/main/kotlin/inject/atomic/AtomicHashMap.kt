package io.effectivelabs.inject.atomic

import java.util.concurrent.atomic.AtomicReference

class AtomicHashMap<K, V>() {
    private val map = AtomicReference(mapOf<K, V>())
    val keys get() = map.get().keys

    fun getOrPut(key: K, defaultValue: () -> V): V {
        return map.get()[key] ?: kotlin.run {
            val value = defaultValue()
            map.updateAndGet { it + (key to value) }
            value
        }
    }
}