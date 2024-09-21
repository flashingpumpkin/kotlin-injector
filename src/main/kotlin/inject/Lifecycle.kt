package io.effectivelabs.inject

interface Lifecycle : AutoCloseable {
    fun start() {}
}