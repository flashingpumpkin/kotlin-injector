package io.effectivelabs.inject

import io.effectivelabs.inject.atomic.AtomicHashMap
import io.effectivelabs.inject.atomic.AtomicList
import io.effectivelabs.inject.atomic.AtomicSet
import io.effectivelabs.inject.exceptions.CircularDependencyException
import io.effectivelabs.inject.exceptions.MissingProviderException
import io.effectivelabs.inject.exceptions.MissingSuitableConstructorException
import io.effectivelabs.inject.exceptions.UnresolvedDependencyException
import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton
import kotlin.reflect.KCallable
import kotlin.reflect.KClass


data class Injector(
    private val instances: AtomicHashMap<KClass<*>, Any> = AtomicHashMap(),
    private val lifecycleComponents: AtomicList<Lifecycle> = AtomicList(),
    private val validatedClasses: AtomicSet<KClass<*>> = AtomicSet(),
    private val modules: List<InjectorModule> = emptyList(),
    private val providers: List<Provider<*>> = emptyList()
) {
    companion object {
        fun create(): Injector = Injector()
    }

    fun registerModule(module: InjectorModule): Injector = copy(modules = listOf(module) + modules)

    fun registerProvider(provider: Provider<*>): Injector = copy(providers = listOf(provider) + providers)

    fun <T : Any> use(block: StartedInjector.() -> T): T {
        return start().use(block)
    }

    fun start() = StartedInjector(this)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> inject(klass: KClass<T>): T {
        validateDependencies(klass)

        return when {
            klass.isSingleton() -> instances.getOrPut(klass) { createInstance(klass, isSingleton = true) } as T
            else -> createInstance(klass, isSingleton = false)
        }
    }

    private fun <T : Any> createInstance(klass: KClass<T>, isSingleton: Boolean): T {

        val constructor = findConstructor(klass)

        val params = constructor.parameters.map { param ->
            inject(param.type.classifier as KClass<*>)
        }

        val instance = constructor.call(*params.toTypedArray())

        if (instance is Lifecycle) {
            if (isSingleton) {
                lifecycleComponents.prepend(instance)
            }
            instance.start()
        }

        return instance
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> findConstructor(klass: KClass<T>): KCallable<T> {
        return (findProvider(klass) ?: findProviderMethod(klass) ?: klass.getConstructor()) as KCallable<T>? ?: run {
            if (klass.isAbstract) {
                throw MissingProviderException(klass)
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

    private fun findProvider(klass: KClass<*>): KCallable<*>? {
        val provider = providers.asSequence().find { provider ->
            provider::class.supertypes.any {
                it.arguments.firstOrNull()?.type?.classifier == klass
            }
        }

        return provider?.let { it::get }
    }

    internal fun validateDependencies(klass: KClass<*>) {
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

    private fun close(): Injector = apply {
        lifecycleComponents.forEach { it.close() }
    }

    class StartedInjector(private val injector: Injector) : AutoCloseable {
        fun <T : Any> getInstance(klass: KClass<T>): T = injector.inject(klass)
        inline fun <reified T : Any> inject(): T = getInstance(T::class)

        override fun close() {
            injector.close()
        }
    }
}

fun <T : Any> KClass<T>.getConstructor(): KCallable<T>? {
    return this.constructors.find { it.annotations.any { it is Inject } }
        ?: this.constructors.firstOrNull()
}

fun KClass<*>.isSingleton() = annotations.any { it is Singleton }
fun KCallable<*>.isProvider() = annotations.any { it is ProviderMethod }

