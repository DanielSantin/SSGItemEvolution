package com.ssg.itemevolution.utils

import com.ssg.itemevolution.ItemEvolutionPlugin
import com.ssg.itemevolution.core.DisposableService
import com.ssg.itemevolution.core.InitializableService
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigManager(private val plugin: ItemEvolutionPlugin) : InitializableService, DisposableService {

    private lateinit var mainConfig: FileConfiguration
    private val customConfigs: MutableMap<String, FileConfiguration> = mutableMapOf()

    private lateinit var evolutionConfig: FileConfiguration

    override fun initialize() {
        plugin.logger.info("[SSG] Inicializando ConfigManager...")

        // Carregar evolution.yml
        evolutionConfig = loadCustomConfig("evolution.yml")

        // (se quiser: carregar messages.yml, enchantments.yml, merchant.yml)
        loadCustomConfig("messages.yml")
        loadCustomConfig("enchantments.yml")
        loadCustomConfig("merchant.yml")
        loadCustomConfig("scoreboard.yml")
    }

    override fun dispose() {
        //saveCustomConfig("evolution.yml")
        plugin.logger.info("[SSG] ConfigManager finalizado.")
    }

    fun getCustomConfig(name: String): FileConfiguration? {
        return customConfigs[name]
    }

    private fun loadCustomConfig(fileName: String): FileConfiguration {
        val file = File(plugin.dataFolder, fileName)
        if (!file.exists()) {
            plugin.saveResource(fileName, false)
        }
        val config = YamlConfiguration.loadConfiguration(file)
        customConfigs[fileName] = config
        return config
    }

//    private fun saveCustomConfig(fileName: String) {
//        val file = File(plugin.dataFolder, fileName)
//        val config = customConfigs[fileName] ?: return
//        config.save(file)
//    }

    fun getEvolutionSettings(): EvolutionSettings {
        val section = evolutionConfig.getConfigurationSection("evolution")!!

        return EvolutionSettings(
            baseMultiplier = section.getDouble("base-multiplier", 1.0),
            levelExponent = section.getDouble("level-exponent", 0.75),
            armorMultiplier = section.getDouble("armor-multiplier", 0.25),
            maxLevel = section.getInt("max-level", 100)
        )
    }

    fun reloadConfigurations() {
        // Recarregar config padrão
        plugin.reloadConfig()
        mainConfig = plugin.config

        // Recarregar evolution.yml
        evolutionConfig = loadCustomConfig("evolution.yml")

        // Recarregar outros configs customizados
        loadCustomConfig("messages.yml")
        loadCustomConfig("enchantments.yml")
        loadCustomConfig("merchant.yml")
        loadCustomConfig("scoreboard.yml")
    }
}

// Modelo de dados para evolution.yml
data class EvolutionSettings(
    val baseMultiplier: Double,
    val levelExponent: Double,
    val armorMultiplier: Double,
    val maxLevel: Int
)
