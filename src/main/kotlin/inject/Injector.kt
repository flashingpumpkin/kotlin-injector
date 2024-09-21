package io.effectivelabs.inject

import io.effectivelabs.inject.exceptions.CircularDependencyException
import io.effectivelabs.inject.exceptions.MissingImplementationException
import io.effectivelabs.inject.exceptions.MissingSuitableConstructorException
import io.effectivelabs.inject.exceptions.UnresolvedDependencyException
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

interface Module

class Injector private constructor() : AutoCloseable {
    private val instances = mutableMapOf<KClass<*>, Any>()
    private val lifecycleComponents = mutableListOf<Lifecycle>()
    private val validatedClasses = mutableSetOf<KClass<*>>()
    private val modules = mutableListOf<Module>()

    companion object {
        fun create(): Injector = Injector()
    }

    fun registerModule(module: Module) = apply {
        modules.add(module)
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

    private fun <T : Any> createInstance(klass: KClass<T>): T {
        val constructor = findConstructor(klass)

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

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> findConstructor(klass: KClass<T>): KCallable<T> {
        return (findProviderMethod(klass) ?: klass.getConstructor()) as KCallable<T>? ?: run {
            if(klass.isAbstract) {
                throw MissingImplementationException(klass)
            } else {
                throw MissingSuitableConstructorException(klass)
            }
        }
    }

    private fun findProviderMethod(klass: KClass<*>): KCallable<*>? {
        val provider: KCallable<*>? = modules.asSequence().flatMap { module ->
            module::class.members.filter { func: KCallable<*> ->
                func.isProvider() &&
                        func.returnType.classifier == klass
            }
        }.firstOrNull()

        return provider
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

        val constructor = findConstructor(klass)

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

    override fun close() {
        lifecycleComponents.reversed().forEach { it.close() }
    }
}

fun <T : Any> KClass<T>.getConstructor(): KCallable<T>? {
    return this.constructors.find { it.annotations.any { it is Inject } }
        ?: this.constructors.firstOrNull()

}

fun KClass<*>.isSingleton() = annotations.any { it is Singleton }
fun KCallable<*>.isProvider() = annotations.any { it is Provider }

