package com.ssg.itemevolution.core

import org.bukkit.plugin.java.JavaPlugin
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

interface InitializableService {
    fun initialize()
}

interface DisposableService {
    fun dispose()
}

/**
 * ServiceContainer completo com injeção de dependência por construtor e gerenciamento de ciclo de vida.
 */
class ServiceContainer(private val plugin: JavaPlugin) {

    private val instances = mutableMapOf<KClass<*>, Any>()
    private val serviceDefinitions = mutableMapOf<KClass<*>, ServiceDefinition>()
    private enum class ServiceType { SINGLETON, FACTORY }
    private data class ServiceDefinition(val type: ServiceType)

    init {
        // Registre a si mesmo, o plugin do Bukkit e a sua classe principal
        instances[ServiceContainer::class] = this
        instances[JavaPlugin::class] = plugin
        instances[plugin::class] = plugin // REGISTRE AQUI!
    }

    fun <T : Any> registerSingleton(serviceClass: KClass<T>) {
        serviceDefinitions[serviceClass] = ServiceDefinition(ServiceType.SINGLETON)
        instances[serviceClass] = createInstance(serviceClass) // CRIA AQUI (sem initialize)
    }

    fun <T : Any> registerFactory(serviceClass: KClass<T>) {
        serviceDefinitions[serviceClass] = ServiceDefinition(ServiceType.FACTORY)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(serviceClass: KClass<T>): T {
        if (serviceDefinitions[serviceClass]?.type == ServiceType.SINGLETON) {
            val instance = instances[serviceClass]
            if (instance != null) {
                return instance as T
            }
        }

        val newInstance = createInstance(serviceClass)

        if (serviceDefinitions[serviceClass]?.type == ServiceType.SINGLETON) {
            instances[serviceClass] = newInstance
        }
        return newInstance
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> createInstance(serviceClass: KClass<T>): T {
        // CORREÇÃO: VERIFICA SE A INSTÂNCIA JÁ EXISTE ANTES DE TENTAR CRIAR
        val existingInstance = instances[serviceClass]
        if (existingInstance != null) {
            return existingInstance as T
        }

        val constructor = serviceClass.primaryConstructor
            ?: throw IllegalArgumentException("A classe ${serviceClass.simpleName} não tem um construtor primário.")

        val dependencies = constructor.parameters.map { param ->
            val dependencyClass = param.type.classifier as? KClass<*>
                ?: throw IllegalArgumentException("Não foi possível resolver a dependência de ${serviceClass.simpleName}")

            get(dependencyClass)
        }
        return constructor.call(*dependencies.toTypedArray())
    }

    fun initializeServices() {
        instances.values.filterIsInstance<InitializableService>().forEach { service ->
            service.initialize()
        }
    }

    fun dispose() {
        instances.values.filterIsInstance<DisposableService>().forEach { service ->
            service.dispose()
        }
        instances.clear()
        serviceDefinitions.clear()
    }
}