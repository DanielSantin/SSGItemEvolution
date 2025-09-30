package com.ssg.itemevolution

import com.ssg.itemevolution.utils.ConfigManager
import com.ssg.itemevolution.core.ServiceContainer
import com.ssg.itemevolution.enchantments.mining.DesabarEnchantment
import com.ssg.itemevolution.enchantments.mining.VeinMiningEnchantment
import com.ssg.itemevolution.enchantments.utility.EternaEnchantment
import com.ssg.itemevolution.ui.SoulToolDialog
import com.ssg.itemevolution.handlers.EventManager
import com.ssg.itemevolution.handlers.CommandHandler
import com.ssg.itemevolution.handlers.EnchantmentRegistrationHandler
import com.ssg.itemevolution.handlers.MerchantHandler
import com.ssg.itemevolution.services.SoulToolService
import com.ssg.itemevolution.listeners.ItemEvolutionListener
import com.ssg.itemevolution.listeners.MerchantInteractionListener
import com.ssg.itemevolution.services.EnchantmentService
import com.ssg.itemevolution.services.ItemDataService
import com.ssg.itemevolution.services.ItemEvolutionService
import com.ssg.itemevolution.ui.ItemDescriptionFormatter
import com.ssg.itemevolution.utils.ItemUtils
import org.bukkit.plugin.java.JavaPlugin

class ItemEvolutionPlugin : JavaPlugin() {
    private lateinit var container: ServiceContainer

    // Em sua classe principal ItemEvolutionPlugin
    override fun onEnable() {
        // 1. Instanciar o container
        container = ServiceContainer(this)
        EternaEnchantment.initializeKeys(this)

        // 2. Registrar todas as suas classes de serviço como Singletons
        container.registerSingleton(ConfigManager::class)
        container.registerSingleton(ItemDataService::class)
        container.registerSingleton(ItemEvolutionService::class)
        container.registerSingleton(ItemDescriptionFormatter::class)
        container.registerSingleton(ItemUtils::class)
        container.registerSingleton(EventManager::class)
        container.registerSingleton(DesabarEnchantment::class)
        container.registerSingleton(VeinMiningEnchantment::class)
        container.registerSingleton(EternaEnchantment::class)
        container.registerSingleton(EnchantmentService::class)
        container.registerSingleton(SoulToolService::class)
        container.registerSingleton(MerchantHandler::class)
        container.registerSingleton(SoulToolDialog::class)
        container.registerSingleton(EnchantmentRegistrationHandler::class)
        container.registerSingleton(ItemEvolutionListener::class)
        container.registerSingleton(MerchantInteractionListener::class)
        container.registerSingleton(CommandHandler::class) // ADICIONE ESTE!

        // 3. PRIMEIRO inicializa todos os serviços
        container.initializeServices()

        // 4. DEPOIS obtém as instâncias (já inicializadas)
        val commandHandler = container.get(CommandHandler::class)
        val enchantmentRegistrationHandler = container.get(EnchantmentRegistrationHandler::class)
        val itemEvolutionListener = container.get(ItemEvolutionListener::class)
        val merchantInteractionListener = container.get(MerchantInteractionListener::class)

        // 5. Ativar os serviços
        this.getCommand("ssgitemevolution")?.setExecutor(commandHandler)
        this.getCommand("ssgitemevolution")?.tabCompleter = commandHandler
        enchantmentRegistrationHandler.registerEnchantmentEventHandlers()
        server.pluginManager.registerEvents(itemEvolutionListener, this)
        server.pluginManager.registerEvents(merchantInteractionListener, this)
    }
    override fun onDisable() {
        container.dispose()
        logger.info("Plugin desabilitado com sucesso!")
    }
}