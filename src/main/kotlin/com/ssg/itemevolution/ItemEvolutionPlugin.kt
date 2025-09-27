package com.ssg.itemevolution

import com.ssg.itemevolution.enchantments.listeners.MiningEnchantmentListener
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ItemEvolutionPlugin : JavaPlugin() {

    companion object {
        lateinit var instance: ItemEvolutionPlugin
            private set

        lateinit var itemEvolutionListener: ItemEvolutionListener
            private set

        var evolutionsConfig: YamlConfiguration? = null
            private set
    }

    override fun onEnable() {
        instance = this

        logger.info("[SSG] Carregando ItemEvolution Plugin")

        // Cria a pasta do plugin se não existir
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        // Carregar configurações
        reloadConfigurations()

        // Registrar eventos
        server.pluginManager.registerEvents(ItemEvolutionListener(), this)
        server.pluginManager.registerEvents(MiningEnchantmentListener(), this)

        // Registrar comandos
        getCommand("ssgitemevolution")?.apply {
            val handler = CommandHandler(this@ItemEvolutionPlugin)
            setExecutor(handler)
            tabCompleter = handler
        }

        itemEvolutionListener = ItemEvolutionListener()

        logger.info("[SSG] ItemEvolution Plugin carregado com sucesso!")
    }

    override fun onDisable() {
        logger.info("[SSG] ItemEvolution Plugin descarregado!")
    }

    fun loadEvolutionsConfig() {
        val configFile = File(dataFolder, "BetterItemsEvolutions.yml")
        if (!configFile.exists()) {
            saveResource("BetterItemsEvolutions.yml", false)
            logger.info("Arquivo BetterItemsEvolutions.yml criado a partir do recurso padrão!")
        }

        evolutionsConfig = YamlConfiguration.loadConfiguration(configFile)
    }

    // Método para recarregar as configurações
    fun reloadConfigurations() {
        reloadConfig() // Recarrega o config.yml padrão
        loadEvolutionsConfig() // Recarrega as evoluções
    }

}