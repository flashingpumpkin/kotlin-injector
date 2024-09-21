package io.effectivelabs.infuser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class InfuserTest {

    @Test
    fun `should inject dependencies`() {
        val infuser = Infuser()
        infuser.register(ServiceA::class)
        infuser.register(ServiceB::class)

        val instance = infuser.getInstance(ServiceA::class)

        assertNotNull(instance)
        assertNotNull(instance.serviceB)
    }

    @Test
    fun `should create singleton instances`() {
        val infuser = Infuser()
        infuser.register(ServiceB::class)

        val instance1 = infuser.getInstance(ServiceB::class)
        val instance2 = infuser.getInstance(ServiceB::class)

        assert(instance1 === instance2)
    }

    @Test
    fun `should throw exception for unregistered dependencies`() {
        val infuser = Infuser()

        assertThrows<IllegalStateException> {
            infuser.getInstance(ServiceA::class)
        }
    }

    @Singleton
    class ServiceB

    class ServiceA @Inject constructor(val serviceB: ServiceB)
}