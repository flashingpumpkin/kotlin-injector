package io.effectivelabs.inject

import io.effectivelabs.inject.exceptions.CircularDependencyException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class InjectorTest {

    @Test
    fun `should inject dependencies`() {
        val injector = Injector.create()

        val instance = injector.inject(ServiceA::class)

        assertNotNull(instance)
        assertNotNull(instance.serviceB)
    }

    @Test
    fun `should inject dependencies correctly`() {
        val injector = Injector.create()

        val serviceA = injector.inject(ServiceA::class)

        assertNotNull(serviceA)
        assertNotNull(serviceA.serviceB)
    }

    @Test
    fun `should create singleton instances`() {
        val injector = Injector.create()

        val instance1 = injector.inject(ServiceB::class)
        val instance2 = injector.inject(ServiceB::class)

        assertSame(instance1, instance2)
    }

    @Test
    fun `should create new instances`() {
        val injector = Injector.create()

        val instance1 = injector.inject(ServiceA::class)
        val instance2 = injector.inject(ServiceA::class)

        assertNotNull(instance1)
        assertNotNull(instance2)
        assertNotSame(instance1, instance2)
    }

    @Test
    fun `should detect circular dependencies`() {
        val injector = Injector.create()

        val result = assertThrows<CircularDependencyException> {
            injector.validateDependencies(CircularA::class)
        }

        assertEquals(
            "Circular dependency detected: io.effectivelabs.inject.InjectorTest.CircularB -> io.effectivelabs.inject.InjectorTest.CircularA",
            result.message
        )
    }

    @Test
    fun `should use lifecycle components`() {
        val service = Injector.create().use {
            it.inject<ServiceWithLifecycle>().also {
                assertTrue(it.started)
                assertFalse(it.stopped)
            }
        }
        assertTrue(service.stopped)
    }

    @Singleton
    class ServiceB
    class ServiceA(val serviceB: ServiceB)
    class CircularA(val circularB: CircularB)
    class CircularB(val circularA: CircularA)

    @Singleton
    class ServiceWithLifecycle : Lifecycle {
        var started = false
        var stopped = false

        override fun start() {
            started = true
        }

        override fun close() {
            stopped = true
        }
    }
}