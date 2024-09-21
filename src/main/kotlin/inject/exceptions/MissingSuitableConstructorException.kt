package io.effectivelabs.inject.exceptions

import kotlin.reflect.KClass

class MissingSuitableConstructorException(val klass: KClass<*>) : IllegalArgumentException("No suitable constructor found for '${klass.qualifiedName}'")
