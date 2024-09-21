package io.effectivelabs.inject.exceptions

import kotlin.reflect.KClass

class CircularDependencyException(val consumer: KClass<*>, dependency: KClass<*>) :
    IllegalStateException("Circular dependency detected: '${consumer.qualifiedName} -> ${dependency.qualifiedName}'")