package com.ssg.itemevolution.utils

import com.ssg.itemevolution.ItemEvolutionPlugin
import com.ssg.itemevolution.core.DisposableService
import com.ssg.itemevolution.core.InitializableService
import com.ssg.itemevolution.core.ServiceContainer
import com.ssg.itemevolution.keys.ToolCategory
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Gerenciador de Configurações do Plugin.
 *
 * Agora só recebe o Plugin no construtor para evitar ciclos de injeção.
 * As dependências circulares são resolvidas usando propriedades 'lateinit'.
 */
class ConfigManager(
    private val plugin: ItemEvolutionPlugin,
    private val container: ServiceContainer
) : InitializableService, DisposableService {

    private lateinit var mainConfig: FileConfiguration
    private val customConfigs: MutableMap<String, FileConfiguration> = mutableMapOf()
    private lateinit var evolutionConfig: FileConfiguration

    override fun initialize() {
        plugin.logger.info("[SSG] Inicializando ConfigManager...")

        // Carregar evolution.yml
        evolutionConfig = loadCustomConfig("evolution.yml")

        // Carregar outros configs customizados
        loadCustomConfig("messages.yml")
        loadCustomConfig("enchantments.yml")
        loadCustomConfig("merchant.yml")
        loadCustomConfig("scoreboard.yml")
        loadCustomConfig("visual_enchantments.yml")

    }


    override fun dispose() {
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

    fun getEvolutionSettings(): EvolutionSettings {
        val section = evolutionConfig.getConfigurationSection("evolution")!!

        return EvolutionSettings(
            maxLevel = section.getInt("max-level", 5),
            minLevel = section.getInt("min-level", 1)
        )
    }

    /**
     * Recarrega todos os arquivos de configuração e notifica os serviços dependentes.
     */
    fun reloadConfigurations() {
        // Recarregar config padrão
        plugin.reloadConfig()
        mainConfig = plugin.config

        // Recarregar todos os configs customizados
        evolutionConfig = loadCustomConfig("evolution.yml")
        loadCustomConfig("messages.yml")
        loadCustomConfig("enchantments.yml")
        loadCustomConfig("merchant.yml")
        loadCustomConfig("scoreboard.yml")
        loadCustomConfig("visual_enchantments.yml")

        // NOTIFICAR SERVIÇOS SOBRE O RELOAD
        // Como o acesso é via lateinit, garantimos que eles estão setados antes de chamar.
        container.reloadServices()
    }

    /**
     * Obtém a fórmula de progressão de uso para um material.
     */
    fun getProgressionFormula(categoryName: String, material: Material): String {
        val fallbackFormula = "({L} - 1)^1.0 * {DUR} * 1.0"
        val materialName = material.name.uppercase()
        val rootSection = evolutionConfig.getConfigurationSection("evolution.use-progression-formula")
            ?: return fallbackFormula

        // --- PRIORIDADE 1: MATERIAL ESPECÍFICO ---
        val categorySection = rootSection.getConfigurationSection(categoryName)
        val materialFormula = categorySection?.getString(materialName)
        if (materialFormula != null) return materialFormula

        // --- PRIORIDADE 2: DEFAULT DA CATEGORIA ---
        val categoryDefaultFormula = categorySection?.getString("_DEFAULT_")
        if (categoryDefaultFormula != null) return categoryDefaultFormula

        // --- PRIORIDADE 3: FALLBACK GLOBAL DE ARMADURA ---
        val category = ToolCategory.fromMaterial(material)
        if (ToolCategory.isArmor(category)) {
            val armorFallbackFormula = rootSection.getString("ARMOR")
            if (armorFallbackFormula != null) return armorFallbackFormula
        }

        // --- PRIORIDADE 4: PADRÃO GLOBAL ---
        return rootSection.getString("DEFAULT", fallbackFormula)
            ?: fallbackFormula
    }

    /**
     * Obtém a lista de pontos de evolução ganhos por nível para um material.
     */
    fun getLevelPointsList(categoryName: String, materialName: String): List<Int> {
        val section = evolutionConfig.getConfigurationSection("evolution.material-level-points")
            ?: return evolutionConfig.getIntegerList("evolution.material-level-points.DEFAULT")

        val categorySection = section.getConfigurationSection(categoryName)

        // 1. Tenta obter a lista específica para o material (ex: DIAMOND_PICKAXE)
        val customList = categorySection?.getIntegerList(materialName)

        if (!customList.isNullOrEmpty()) {
            return customList
        }

        // 2. Tenta o Fallback DEFAULT
        val defaultList = section.getIntegerList("DEFAULT")

        return defaultList
    }
}

// Modelo de dados para evolution.yml
data class EvolutionSettings(
    val maxLevel: Int,
    val minLevel: Int
)