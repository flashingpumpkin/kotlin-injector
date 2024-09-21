package io.effectivelabs.inject.exceptions

import kotlin.reflect.KClass

class UnresolvedDependencyException(val klass: KClass<*>) : IllegalStateException("Cannot resolve dependency for '${klass.qualifiedName}'")