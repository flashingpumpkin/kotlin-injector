package io.effectivelabs.inject.exceptions

import kotlin.reflect.KClass

class MissingProviderException(val klass: KClass<*>) :
    IllegalArgumentException("No provider for abstract class '${klass.qualifiedName}' found. Please use @ProviderMethod to provide an instance.")