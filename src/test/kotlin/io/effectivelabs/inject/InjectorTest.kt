package io.effectivelabs.inject

import io.effectivelabs.inject.exceptions.CircularDependencyException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@ExtendWith(MockitoExtension::class)
class InjectorTest {

    @Test
    fun `should inject dependencies`() {
        val infuser = Injector.create()

        val instance = infuser.inject(ServiceA::class)

        assertNotNull(instance)
        assertNotNull(instance.serviceB)
    }

    @Test
    fun `should inject dependencies correctly`() {
        val infuser = Injector.create()

        val serviceA = infuser.inject(ServiceA::class)

        assertNotNull(serviceA)
        assertNotNull(serviceA.serviceB)
    }

    @Test
    fun `should create singleton instances`() {
        val infuser = Injector.create()

        val instance1 = infuser.inject(ServiceB::class)
        val instance2 = infuser.inject(ServiceB::class)

        assertSame(instance1, instance2)
    }

    @Test
    fun `should detect circular dependencies`() {
        val infuser = Injector.create()

        val result = assertThrows<CircularDependencyException> {
            infuser.validateDependencies(CircularA::class)
        }

        assertEquals(
            "Circular dependency detected: io.effectivelabs.inject.InjectorTest.CircularB -> io.effectivelabs.inject.InjectorTest.CircularA",
            result.message
        )
    }

//    @Test
//    fun `should work with custom providers`() {
//        val infuser = Injector.create()
//        infuser.registerProvider(String::class) { "Hello, DI!" }
//
//        val result = infuser.inject(String::class)
//
//        Assertions.assertEquals("Hello, DI!", result)
//    }

    @Singleton
    class ServiceB
    class ServiceA(val serviceB: ServiceB)
    class CircularA(val circularB: CircularB)
    class CircularB(val circularA: CircularA)
}