package io.effectivelabs.infuser


import kotlin.reflect.KClass

class Infuser {
    private val instances: MutableMap<KClass<*>, Any> = mutableMapOf()
    private val registeredClasses: MutableSet<KClass<*>> = mutableSetOf()

    fun <T : Any> register(klass: KClass<T>) {
        registeredClasses.add(klass)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getInstance(klass: KClass<T>): T {
        if (!registeredClasses.contains(klass)) {
            throw IllegalStateException("Class ${klass.simpleName} is not registered")
        }

        return instances.getOrPut(klass) {
            createInstance(klass)
        } as T
    }

    private fun <T : Any> createInstance(klass: KClass<T>): T {
        val constructor = klass.constructors.find { it.annotations.any { a -> a is Inject } }
            ?: klass.constructors.firstOrNull()
            ?: throw IllegalStateException("No suitable constructor found for ${klass.simpleName}")

        val params = constructor.parameters.map { param ->
            getInstance(param.type.classifier as KClass<*>)
        }

        val instance = constructor.call(*params.toTypedArray())

        if (klass.annotations.any { it is Singleton }) {
            instances[klass] = instance
        }

        return instance
    }
}
