package io.effectivelabs.inject.exceptions

import kotlin.reflect.KClass

class MissingDependencyException(val consumer: KClass<*>, val dependency: KClass<*>) : IllegalStateException("Cannot provide dependency ${dependency.qualifiedName} for ${consumer.qualifiedName}")