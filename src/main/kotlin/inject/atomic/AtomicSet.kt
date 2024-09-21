package io.effectivelabs.inject.atomic

import java.util.concurrent.atomic.AtomicReference

class AtomicSet<T> {
    private val set = AtomicReference(setOf<T>())

    fun add(element: T): Boolean {
        return set.updateAndGet { it + element }.contains(element)
    }

    operator fun contains(element: T): Boolean {
        return set.get().contains(element)
    }
}