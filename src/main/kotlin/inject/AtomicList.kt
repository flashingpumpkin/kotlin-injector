package io.effectivelabs.inject

import java.util.concurrent.atomic.AtomicReference

class AtomicList<T> {
    private val list = AtomicReference(listOf<T>())

    fun prepend(element: T): List<T> {
        return list.updateAndGet { listOf(element) + it }
    }

    fun asSequence(): Sequence<T> {
        return list.get().asSequence()
    }

    fun forEach(action: (T) -> Unit) {
        list.get().forEach(action)
    }

    operator fun contains(element: T): Boolean {
        return list.get().contains(element)
    }

}