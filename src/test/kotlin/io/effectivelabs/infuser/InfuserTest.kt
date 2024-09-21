package io.effectivelabs.infuser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class InfuserTest {

    @Test
    fun `should inject dependencies`() {
        val infuser = Infuser()

        val instance = infuser.getInstance(ServiceA::class)

        assertNotNull(instance)
        assertNotNull(instance.serviceB)
    }

    @Test
    fun `should inject dependencies correctly`() {
        val infuser = Infuser()

        val serviceA = infuser.getInstance(ServiceA::class)

        assertNotNull(serviceA)
        assertNotNull(serviceA.serviceB)
    }

    @Test
    fun `should create singleton instances`() {
        val infuser = Infuser()

        val instance1 = infuser.getInstance(ServiceB::class)
        val instance2 = infuser.getInstance(ServiceB::class)

        assertTrue(instance1 === instance2)
    }

    @Test
    fun `should detect circular dependencies`() {
        val infuser = Infuser()

        assertThrows<CircularDependencyException> {
            infuser.validateDependencies(CircularA::class)
        }
    }

    @Test
    fun `should work with custom providers`() {
        val infuser = Infuser()
        infuser.registerProvider(String::class) { "Hello, DI!" }

        val result = infuser.getInstance(String::class)

        assertEquals("Hello, DI!", result)
    }

    @Singleton
    class ServiceB

    @Inject
    class ServiceA(val serviceB: ServiceB)

    @Inject
    class CircularA(val circularB: CircularB)

    @Inject
    class CircularB(val circularA: CircularA)
}