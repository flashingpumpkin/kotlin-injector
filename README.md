# kotlin-injector

Very simple dependency injection container for Kotlin.

Usage:

```kotlin
import io.effectivelabs.inject.Injector
import io.effectivelabs.inject.InjectorModule
import io.effectivelabs.inject.Lifecycle
import jakarta.inject.Provider
import jakarta.inject.Singleton

@Singleton
class ServiceA

class ServiceB(val serviceA: ServiceA)

interface ServiceC

interface ServiceD

class ServiceE(
    val serviceA: ServiceA,
    val serviceB: ServiceB,
    val serviceC: ServiceC,
    val serviceD: ServiceD
)

class ServiceModule : InjectorModule {
    @ProviderMethod
    fun provideServiceC(): ServiceC {
        return object : ServiceC {}
    }
}

class ServiceProvider : Provider<ServiceD> {
    override fun get(): ServiceD {
        return object : ServiceD {}
    }
}

@Singleton
class ServiceWithLifecycle(serviceE: ServiceE) : Lifecycle {
    var started = false
    var stopped = false

    fun doWork() {
        println("ServiceWithLifecycle is doing work")
    }

    override fun start() {
        started = true
    }

    override fun close() {
        stopped = true
    }

}

Injector.create()
    .registerModule(ServiceModule())
    .registerProvider(ServiceProvider())
    .use { 
        val serviceWithLifecycle = inject<ServiceWithLifecycle>()
        serviceWithLifecycle.doWork()
    }
```
