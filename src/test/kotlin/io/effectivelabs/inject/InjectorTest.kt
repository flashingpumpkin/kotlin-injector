package io.effectivelabs.inject

import io.effectivelabs.inject.exceptions.CircularDependencyException
import io.effectivelabs.inject.exceptions.MissingProviderException
import jakarta.inject.Singleton
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class InjectorTest {

    @Test
    fun `should inject dependencies`() {
        val injector = Injector.create().start()

        val instance = injector.inject<ServiceA>()

        assertNotNull(instance)
        assertNotNull(instance.serviceB)
    }

    @Test
    fun `should inject dependencies correctly`() {
        val injector = Injector.create().start()

        val serviceA = injector.inject<ServiceA>()

        assertNotNull(serviceA)
        assertNotNull(serviceA.serviceB)
    }

    @Test
    fun `should create singleton instances`() {
        val injector = Injector.create().start()

        val instance1: ServiceB = injector.inject()
        val instance2: ServiceB = injector.inject()

        assertSame(instance1, instance2)
    }

    @Test
    fun `should create new instances`() {
        val injector = Injector.create().start()

        val instance1: ServiceA = injector.inject()
        val instance2: ServiceA = injector.inject()

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
        val service = Injector.create().start().use {
            it.inject<ServiceWithLifecycle>().also {
                assertTrue(it.started)
                assertFalse(it.stopped)
            }
        }
        assertTrue(service.stopped)
    }

    @Test
    fun `should inject dependencies from module`() {
        val injector = Injector.create()
            .registerModule(TestModule())
            .start()

        val instance = injector.inject<ModuleDependency>()

        assertNotNull(instance)
        assertEquals("Test Dependency", instance.value)
    }

    @Test
    fun `should inject dependencies with arguments from module`() {
        val injector = Injector.create()
            .registerModule(TestModule())
            .start()

        val instance = injector.inject<ModuleMultiArgService>()

        assertNotNull(instance)
        assertEquals("Test Dependency", instance.moduleDependency.value)
        assertEquals("Test Dependency", instance.moduleService.dependency.value)
        assertEquals("Injected Arg", instance.moduleService.arg)
    }

    @Test
    fun `should throw error on missing implementation`() {
        val injector = Injector.create()
            .registerModule(TestModule())
            .start()

        val result = assertThrows<MissingProviderException> {
            injector.inject<MissingImplementationService>()
        }

        assertEquals(
            "No provider for abstract class 'io.effectivelabs.inject.InjectorTest.MissingImplementation' found. Please use @ProviderMethod to provide an instance.",
            result.message
        )
    }

    @Test
    fun `should inject dependencies from module with implementation provider`() {
        val injector = Injector.create()
            .registerModule(ImplementationProvider())
            .start()

        val instance = injector.inject<MissingImplementation>()

        assertNotNull(instance)
        assertIs<ImplementationProvider.Impl>(instance)
    }

    @Test
    fun `should be able to override provided implementations`() {
        val injector = Injector.create()
            .registerModule(TestModule())
            .registerModule(ImplementationProvider())
            .registerModule(OverrideProvider())
            .start()

        val instance = injector.inject<MissingImplementation>()

        assertNotNull(instance)
        assertIs<OverrideProvider.Impl>(instance)
    }

    class TestModule : Module {
        @ProviderMethod
        fun provideModuleDependency(): ModuleDependency {
            return ModuleDependency("Test Dependency")
        }

        @ProviderMethod
        fun provideModuleService(dependency: ModuleDependency): ModuleService {
            return ModuleService("Injected Arg", dependency)
        }

        @ProviderMethod
        fun provideMultiArgService(
            moduleDependency: ModuleDependency,
            moduleService: ModuleService
        ): ModuleMultiArgService {
            return ModuleMultiArgService(moduleDependency, moduleService)
        }
    }

    class ImplementationProvider : Module {
        class Impl : MissingImplementation

        @ProviderMethod
        fun provideMissingImplementation(): MissingImplementation {
            return Impl()
        }
    }

    class OverrideProvider : Module {
        class Impl : MissingImplementation

        @ProviderMethod
        fun provideMissingImplementation(): MissingImplementation {
            return Impl()
        }
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

    class ModuleDependency(val value: String)
    class ModuleService(val arg: String, val dependency: ModuleDependency)
    class ModuleMultiArgService(val moduleDependency: ModuleDependency, val moduleService: ModuleService)

    interface MissingImplementation
    class MissingImplementationService(val missing: MissingImplementation)
}