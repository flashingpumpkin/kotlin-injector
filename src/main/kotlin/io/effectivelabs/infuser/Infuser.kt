package io.effectivelabs.infuser


import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class Infuser {
    private val instances: MutableMap<KClass<*>, Any> = mutableMapOf()
    private val providers: MutableMap<KClass<*>, () -> Any> = mutableMapOf()
    private val registeredClasses: MutableSet<KClass<*>> = mutableSetOf()

    fun <T : Any> register(klass: KClass<T>) {
        registeredClasses.add(klass)
    }

    fun <T : Any> registerProvider(klass: KClass<T>, provider: () -> T) {
        providers[klass] = provider
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getInstance(klass: KClass<T>, qualifier: String? = null): T {
        if (!registeredClasses.contains(klass) && !providers.containsKey(klass)) {
            throw UnresolvedDependencyException(klass)
        }

        return when {
            providers.containsKey(klass) -> providers[klass]!!.invoke() as T
            klass.annotations.any { it is Singleton } -> instances.getOrPut(klass) { createInstance(klass, qualifier) } as T
            else -> createInstance(klass, qualifier)
        }
    }

    private fun <T : Any> createInstance(klass: KClass<T>, qualifier: String?): T {
        val constructor = findSuitableConstructor(klass)

        val params = constructor.parameters.map { param ->
            val paramQualifier = param.annotations.filterIsInstance<Named>().firstOrNull()?.value ?: qualifier
            getInstance(param.type.classifier as KClass<*>, paramQualifier)
        }

        return constructor.call(*params.toTypedArray())
    }

    private fun <T : Any> findSuitableConstructor(klass: KClass<T>): KFunction<T> {
        return klass.constructors.find { it.annotations.any { anno -> anno is Inject } }
            ?: klass.constructors.firstOrNull()
            ?: throw IllegalStateException("No suitable constructor found for ${klass.simpleName}")
    }

    fun validateDependencies() {
        val visited = mutableSetOf<KClass<*>>()
        val recursionStack = mutableSetOf<KClass<*>>()

        registeredClasses.forEach { klass ->
            if (klass !in visited) {
                validateDependenciesRecursive(klass, visited, recursionStack)
            }
        }
    }

    private fun validateDependenciesRecursive(
        klass: KClass<*>,
        visited: MutableSet<KClass<*>>,
        recursionStack: MutableSet<KClass<*>>
    ) {
        visited.add(klass)
        recursionStack.add(klass)

        val constructor = findSuitableConstructor(klass)

        constructor.parameters.forEach { param ->
            val dependencyClass = param.type.classifier as? KClass<*>
                ?: throw UnresolvedDependencyException(klass)

            if (!canProvide(dependencyClass)) {
                throw MissingDependencyException(klass, dependencyClass)
            }

            if (dependencyClass in recursionStack) {
                throw CircularDependencyException(dependencyClass)
            }

            if (dependencyClass !in visited && dependencyClass in registeredClasses) {
                validateDependenciesRecursive(dependencyClass, visited, recursionStack)
            }
        }

        recursionStack.remove(klass)
    }

    private fun canProvide(klass: KClass<*>): Boolean {
        return klass in registeredClasses || klass in providers
    }


}

class UnresolvedDependencyException(val klass: KClass<*>) : IllegalStateException("Cannot resolve dependency for ${klass}")
class MissingDependencyException(val consumer: KClass<*>, val dependency: KClass<*>) : IllegalStateException("Cannot provide dependency ${dependency} for ${consumer}")
class CircularDependencyException(val klass: KClass<*>) : IllegalStateException("Circular dependency detected: $klass")