package com.ssg.itemevolution.utils

import com.ssg.itemevolution.ItemEvolutionPlugin
import com.ssg.itemevolution.core.DisposableService
import com.ssg.itemevolution.core.InitializableService
import com.ssg.itemevolution.keys.ToolCategory
import com.ssg.itemevolution.keys.ToolType
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import kotlin.math.floor

class ConfigManager(private val plugin: ItemEvolutionPlugin) : InitializableService, DisposableService {
    private lateinit var mainConfig: FileConfiguration
    private val customConfigs: MutableMap<String, FileConfiguration> = mutableMapOf()
    private val levelUseTable: MutableMap<Material, List<Int>> = mutableMapOf()
    private lateinit var evolutionConfig: FileConfiguration

    private var scoreboardDefaultTitle: String = ""
    private var scoreboardDefaultLines: List<String> = emptyList()

    private var scoreboardMaxTitle: String = ""
    private var scoreboardMaxLines: List<String> = emptyList()

    override fun initialize() {
        plugin.logger.info("[SSG] Inicializando ConfigManager...")

        // Carregar evolution.yml
        evolutionConfig = loadCustomConfig("evolution.yml")

        // (se quiser: carregar messages.yml, enchantments.yml, merchant.yml)
        loadCustomConfig("messages.yml")
        loadCustomConfig("enchantments.yml")
        loadCustomConfig("merchant.yml")
        loadCustomConfig("scoreboard.yml")

        loadScoreboardConfig()
        generateLevelUseTable()
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

    fun getEvolutionSettings(): EvolutionSettings {
        val section = evolutionConfig.getConfigurationSection("evolution")!!

        return EvolutionSettings(
            maxLevel = section.getInt("max-level", 5),
            minLevel = section.getInt("min-level", 1)
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

        loadScoreboardConfig()
        generateLevelUseTable()
    }

    fun getLevelPointsList(categoryName: String, materialName: String): List<Int> {
        val section = evolutionConfig.getConfigurationSection("evolution.material-level-points")
            ?: return evolutionConfig.getIntegerList("evolution.material-level-points.DEFAULT")

        // Tenta obter a seção da categoria (ex: PICKAXE)
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

    private fun getProgressionFormula(categoryName: String, material: Material): String { // <--- Adicionando o Material para o isArmor
        val fallbackFormula = "({L} - 1)^1.0 * {DUR} * 1.0"
        val materialName = material.name.uppercase()
        val rootSection = evolutionConfig.getConfigurationSection("evolution.use-progression-formula")
            ?: return fallbackFormula

        // --- PRIORIDADE 1: MATERIAL ESPECÍFICO ---

        // 1. Tenta buscar a fórmula específica DENTRO da seção da categoria (Ex: HELMET.DIAMOND_HELMET)
        val categorySection = rootSection.getConfigurationSection(categoryName)
        val materialFormula = categorySection?.getString(materialName)
        if (materialFormula != null) return materialFormula


        // --- PRIORIDADE 2: DEFAULT DA CATEGORIA (NOVA BUSCA) ---

        // 2. Tenta pegar o fallback padrão para a Categoria (Ex: HELMET._DEFAULT_)
        val categoryDefaultFormula = categorySection?.getString("_DEFAULT_")
        if (categoryDefaultFormula != null) return categoryDefaultFormula


        // --- PRIORIDADE 3: FALLBACK GLOBAL DE ARMADURA ---

        val category = ToolCategory.fromMaterial(material) // Usa o objeto Material para checar
        if (ToolCategory.isArmor(category)) {
            val armorFallbackFormula = rootSection.getString("ARMOR")
            if (armorFallbackFormula != null) return armorFallbackFormula
        }


        // --- PRIORIDADE 4: PADRÃO GLOBAL ---

        return rootSection.getString("DEFAULT", fallbackFormula)
            ?: fallbackFormula
    }

    private fun generateLevelUseTable() {
        levelUseTable.clear()
        val settings = getEvolutionSettings()
        val allValidMaterials = ToolType.getAllToolMaterials()

        for (material in allValidMaterials) {
            val durability = material.maxDurability.toDouble()
            val categoryName = ToolCategory.getName(material)

            // NOVO: Obtém a fórmula customizada
            val formula = getProgressionFormula(categoryName, material)

            val useList = mutableListOf<Int>()

            for (level in settings.minLevel..settings.maxLevel) {

                // NOVO: Usa o helper para avaliar a fórmula
                val counterCalculated = FormulaHelper.evaluateCounter(
                    formula = formula,
                    level = level,
                    durability = durability
                )

                // O uso real é o valor avaliado, ajustado pelo multiplicador (se não estiver na fórmula)
                // e arredondado para o inteiro.
                val counterForLevel = floor(counterCalculated).toInt()

                useList.add(counterForLevel)
            }

            levelUseTable[material] = useList
        }
    }

    fun getCounterListForMaterial(material: Material): List<Int>? {
        return levelUseTable[material]
    }

    fun loadScoreboardConfig() {
        val config = getCustomConfig("scoreboard.yml") ?: return

        val defaultSection = config.getConfigurationSection("scoreboard.default")
        scoreboardDefaultTitle = defaultSection?.getString("title") ?: ""
        scoreboardDefaultLines = defaultSection?.getStringList("lines") ?: emptyList()

        val maxSection = config.getConfigurationSection("scoreboard.max-level")
        scoreboardMaxTitle = maxSection?.getString("title") ?: ""
        scoreboardMaxLines = maxSection?.getStringList("lines") ?: emptyList()
    }

    fun getScoreboardTitle(isMaxLvl: Boolean): String {
        return if (isMaxLvl) scoreboardMaxTitle else scoreboardDefaultTitle
    }

    fun getScoreboardLines(isMaxLvl: Boolean): List<String> {
        return if (isMaxLvl) scoreboardMaxLines else scoreboardDefaultLines
    }

}

// Modelo de dados para evolution.yml
data class EvolutionSettings(
    val maxLevel: Int,
    val minLevel: Int
)
