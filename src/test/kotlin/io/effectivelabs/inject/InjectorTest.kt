package io.effectivelabs.inject

import io.effectivelabs.inject.exceptions.CircularDependencyException
import io.effectivelabs.inject.exceptions.MissingProviderException
import jakarta.inject.Provider
import jakarta.inject.Singleton
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InjectorTest {

    @Test
    fun `should inject dependencies`() {
        val injector = Injector.create().start()

        val instance = injector.inject<ServiceB>()

        assertNotNull(instance)
        assertNotNull(instance.serviceA)
    }

    @Test
    fun `should inject multiple dependencies`() {
        val injector = Injector.create()
            .registerModule(ServiceCProvider())
            .start()

        val instance = injector.inject<ServiceD>()

        assertNotNull(instance)
        assertNotNull(instance.serviceA)
        assertIs<ServiceA>(instance.serviceA)
        assertNotNull(instance.serviceB)
        assertIs<ServiceB>(instance.serviceB)
    }

    @Test
    fun `should create singleton instances`() {
        val injector = Injector.create().start()

        val instance1: ServiceA = injector.inject()
        val instance2: ServiceA = injector.inject()

        assertSame(instance1, instance2)
    }

    @Test
    fun `should create new instances`() {
        val injector = Injector.create().start()

        val instance1: ServiceB = injector.inject()
        val instance2: ServiceB = injector.inject()

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
            "Circular dependency detected: 'io.effectivelabs.inject.InjectorTest.CircularB -> io.effectivelabs.inject.InjectorTest.CircularA'",
            result.message
        )
    }

    @Test
    fun `should use lifecycles`() {
        val service = Injector.create().use {
            inject<ServiceWithLifecycle>().also {
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
    fun `should inject dependencies from provider`() {
        val injector = Injector.create()
            .registerProvider(CustomProvider())
            .start()

        val instance = injector.inject<FromProvider>()

        assertNotNull(instance)
        assertIs<FromProvider>(instance)
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
            "No provider for abstract class 'io.effectivelabs.inject.InjectorTest.MissingImplementation' found. Please provide an instance.",
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

    @Test
    fun `should work with Java classes`() {
        val injector = Injector.create()
            .registerModule(UuidModule())
            .start()

        val instance = injector.inject<UUID>()

        assertNotNull(instance)
        assertEquals("1b314e65-4e15-4aa7-b972-b8d1b09db90f", instance.toString())
    }

    class TestModule : InjectorModule {
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

    class ImplementationProvider : InjectorModule {
        class Impl : MissingImplementation

        @ProviderMethod
        fun provideMissingImplementation(): MissingImplementation {
            return Impl()
        }
    }

    class OverrideProvider : InjectorModule {
        class Impl : MissingImplementation

        @ProviderMethod
        fun provideMissingImplementation(): MissingImplementation {
            return Impl()
        }
    }

    @Singleton
    class ServiceA
    class ServiceB(val serviceA: ServiceA)
    interface ServiceC
    class ServiceD(val serviceA: ServiceA, val serviceB: ServiceB, serviceC: ServiceC)
    class CircularA(val circularB: CircularB)
    class CircularB(val circularA: CircularA)

    class ServiceCProvider : InjectorModule {
        @ProviderMethod
        fun provideServiceC(): ServiceC {
            return object : ServiceC {}
        }
    }

    class UuidModule : InjectorModule {
        @ProviderMethod
        fun provideUuid(): UUID {
            return UUID.fromString("1b314e65-4e15-4aa7-b972-b8d1b09db90f")
        }
    }

    @Singleton
    class ServiceWithLifecycle(val serviceA: ServiceA) : Lifecycle {
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

    interface FromProvider {
        val name: String
    }

    class CustomProvider : Provider<FromProvider> {
        override fun get(): FromProvider {
            return object : FromProvider {
                override val name: String = "Custom Provider"
                override fun toString(): String {
                    return "Anonymous($name)"
                }
            }
        }
    }
}