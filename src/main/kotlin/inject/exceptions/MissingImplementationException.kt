package io.effectivelabs.inject.exceptions

import kotlin.reflect.KClass

class MissingImplementationException(val klass: KClass<*>) : IllegalArgumentException("No implementation found for ${klass.qualifiedName}")