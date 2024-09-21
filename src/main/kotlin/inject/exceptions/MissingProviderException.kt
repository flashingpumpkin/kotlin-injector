package io.effectivelabs.inject.exceptions

import kotlin.reflect.KClass

class MissingProviderException(val klass: KClass<*>) :
    IllegalArgumentException("No provider for abstract class '${klass.qualifiedName}' found. Please provide an instance.")