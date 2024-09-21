package io.effectivelabs.infuser


import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class Infuser {
    private val instances: MutableMap<KClass<*>, Any> = mutableMapOf()
    private val providers: MutableMap<KClass<*>, () -> Any> = mutableMapOf()

    fun <T : Any> registerProvider(klass: KClass<T>, provider: () -> T) {
        providers[klass] = provider
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getInstance(klass: KClass<T>, qualifier: String? = null): T {
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
            val paramClass = param.type.classifier as? KClass<*>
                ?: throw UnresolvedDependencyException(klass)
            try {
                getInstance(paramClass, paramQualifier)
            } catch (e: Exception) {
                throw MissingDependencyException(klass, paramClass)
            }
        }

        return constructor.call(*params.toTypedArray())
    }

    private fun <T : Any> findSuitableConstructor(klass: KClass<T>): KFunction<T> {
        return klass.constructors.find { it.annotations.any { anno -> anno is Inject } }
            ?: klass.constructors.firstOrNull()
            ?: throw UnresolvedDependencyException(klass)
    }

    fun validateDependencies(rootClass: KClass<*>) {
        val visited = mutableSetOf<KClass<*>>()
        val recursionStack = mutableSetOf<KClass<*>>()
        validateDependenciesRecursive(rootClass, visited, recursionStack)
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

            if (dependencyClass in recursionStack) {
                throw CircularDependencyException(dependencyClass)
            }

            if (dependencyClass !in visited) {
                validateDependenciesRecursive(dependencyClass, visited, recursionStack)
            }
        }

        recursionStack.remove(klass)
    }
}

class UnresolvedDependencyException(val klass: KClass<*>) : IllegalStateException("Cannot resolve dependency for ${klass}")
class MissingDependencyException(val consumer: KClass<*>, val dependency: KClass<*>) : IllegalStateException("Cannot provide dependency ${dependency} for ${consumer}")
class CircularDependencyException(val klass: KClass<*>) : IllegalStateException("Circular dependency detected: $klass")