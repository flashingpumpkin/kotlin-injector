package io.effectivelabs.inject

import io.effectivelabs.inject.exceptions.CircularDependencyException
import io.effectivelabs.inject.exceptions.MissingSuitableConstructorException
import io.effectivelabs.inject.exceptions.UnresolvedDependencyException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class Injector private constructor() : AutoCloseable {
    private val instances = mutableMapOf<KClass<*>, Any>()
    private val lifecycleComponents = mutableListOf<Lifecycle>()
    private val validatedClasses = mutableSetOf<KClass<*>>()

    companion object {
        fun create(): Injector = Injector()
    }

    inline fun <reified T : Any> inject(): T = inject(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> inject(klass: KClass<T>): T {
        validateDependencies(klass)

        return when {
            klass.isSingleton() -> instances.getOrPut(klass) { createInstance(klass) } as T
            else -> createInstance(klass)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getInstance(klass: KClass<T>): T? {
        return instances.get(klass) as T?
    }

    private fun <T : Any> createInstance(klass: KClass<T>): T {
        val constructor = klass.getConstructor()

        val params = constructor.parameters.map { param ->
            inject(param.type.classifier as KClass<*>)
        }

        val instance = constructor.call(*params.toTypedArray())
        if (instance is Lifecycle) {
            lifecycleComponents.add(instance)
            instance.start()
        }
        return instance
    }


    fun validateDependencies(klass: KClass<*>) {
        validateDependenciesRecursive(klass, mutableSetOf<KClass<*>>())
    }

    private fun validateDependenciesRecursive(
        klass: KClass<*>,
        recursionStack: MutableSet<KClass<*>>
    ) {
        if (klass in validatedClasses) return
        validatedClasses.add(klass)
        recursionStack.add(klass)

        val constructor = klass.getConstructor()

        constructor.parameters.forEach { param ->
            val dependencyClass = param.type.classifier as? KClass<*>
                ?: throw UnresolvedDependencyException(klass)

            if (dependencyClass in recursionStack) {
                throw CircularDependencyException(klass, dependencyClass)
            }

            if (dependencyClass !in validatedClasses) {
                validateDependenciesRecursive(dependencyClass, recursionStack)
            }
        }
        recursionStack.remove(klass)
    }

    fun validateAllDependencies() {
        instances.keys.forEach { validateDependencies(it) }
    }

    fun start() = apply { validateAllDependencies() }

    fun stop() {
        close()
    }

    override fun close() {
        lifecycleComponents.reversed().forEach { it.close() }
    }
}

fun <T : Any> KClass<T>.getConstructor(): KFunction<T> {
    return this.constructors.find { it.annotations.any { it is Inject } }
        ?: this.constructors.firstOrNull()
        ?: throw MissingSuitableConstructorException(this)
}

fun KClass<*>.isSingleton() = annotations.any { it is Singleton }
