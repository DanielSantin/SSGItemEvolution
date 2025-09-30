package com.ssg.itemevolution.services

import com.ssg.itemevolution.utils.ConfigManager
import com.ssg.itemevolution.keys.ToolCategory
import com.ssg.itemevolution.keys.ToolType
import com.ssg.itemevolution.utils.EvolutionSettings
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.math.floor
import kotlin.math.pow

/**
 * Serviço responsável pela lógica de evolução de itens.
 * Centraliza todos os cálculos relacionados a níveis, pontos e progressão.
 */
class ItemEvolutionService(
    private val configManager: ConfigManager,
    private val itemDataService: ItemDataService,
    private val itemMetaTransferService: ItemMetaTransferService,
    private val materialUpgradeService: MaterialUpgradeService
) {
    companion object {
        private const val MIN_LEVEL = 1
    }

    /**
     * Configura um item novo com dados de evolução iniciais
     */
    fun setupItem(item: ItemStack): ItemStack {
        itemDataService.apply {
            setSoulTool(item, true)
            setCounter(item, 1)
            setUses(item, 0)
            setPoints(item, 0)
        }
        return item
    }

    /**
     * Atualiza o item após uso, incrementando contador e verificando level up
     */
    fun upgradeItem(item: ItemStack): ItemStack {
        // 1. Obter os dados atuais do item
        val currentCounter = itemDataService.getCounter(item)
        val currentUses = itemDataService.getUses(item)

        val newCounter = currentCounter + 1
        val newUses = currentUses + 1

        val oldLevel = calculateLevelFromCounter(currentCounter, item)
        val newLevel = calculateLevelFromCounter(newCounter, item)

        itemDataService.setCounter(item, newCounter)
        itemDataService.setUses(item, newUses)

        if (newLevel > oldLevel) {
            val pointsToAdd = calculatePointsGained(item, oldLevel, newLevel)
            itemDataService.addPoints(item, pointsToAdd)
        }

        return item
    }

    /**
     * Melhora o item para o próximo material (ex: ferro -> diamante)
     */
    fun improveItem(item: ItemStack): ItemStack {
        val nextMaterial = materialUpgradeService.getNextMaterial(item.type) ?: return item

        // Criar novo item com material melhorado
        val newItem = ItemStack(nextMaterial, item.amount)

        // Transferir metadados
        itemMetaTransferService.transferMetadata(item, newItem, itemDataService)

        // Resetar contador (começa evolução do zero)
        itemDataService.setCounter(newItem, 1)

        // Copiar encantamentos
        if (item.enchantments.isNotEmpty()) {
            newItem.addUnsafeEnchantments(item.enchantments)
        }

        return newItem
    }

    /**
     * Define o nível do item (ajustando o counter correspondente)
     */
    fun setLevel(item: ItemStack, level: Int) {
        require(level >= 1) { "Level deve ser no mínimo 1" }
        val newCounter = calculateCounterForLevel(level, item) + 1
        itemDataService.setCounter(item, newCounter)
    }

    /**
     * Define o counter baseado em um nível e adiciona ao counter atual
     */
    fun setCounterBasedOnLevel(item: ItemStack, counter: Int) {
        require(counter >= 1) { "Counter deve ser no mínimo 1" }
        val level = calculateLevelFromItem(item)
        val initialCounter = calculateCounterForLevel(level, item)
        val newCounter = initialCounter + counter
        itemDataService.setCounter(item, newCounter)
    }

    fun calculateLevelFromItem(item: ItemStack): Int {
        val counter = itemDataService.getCounter(item)
        return calculateLevelFromCounter(counter, item)
    }

    /**
     * Calcula o nível baseado no contador atual
     */
    fun calculateLevelFromCounter(counter: Int, item: ItemStack): Int {
        val settings = configManager.getEvolutionSettings()
        val durability = item.type.maxDurability.toDouble()
        val multiplier = getMultiplierForItem(item, settings)

        val level = floor(
            ((counter / (durability * multiplier)).pow(settings.levelExponent)) + 1
        ).toInt()

        return level.coerceIn(MIN_LEVEL, settings.maxLevel)
    }

    /**
     * Calcula quantos contadores são necessários para atingir um nível específico
     */
    fun calculateCounterForLevel(level: Int, item: ItemStack): Int {
        val settings = configManager.getEvolutionSettings()
        val durability = item.type.maxDurability.toDouble()
        val multiplier = getMultiplierForItem(item, settings)

        // Inverte a fórmula: counter = (level - 1)^(1/exponent) * durability * multiplier
        val exponent = settings.levelExponent
        return floor(
            (level - 1).toDouble().pow(1.0 / exponent) * durability * multiplier
        ).toInt()
    }

    /**
     * Calcula o progresso até o próximo nível (0.0 a 1.0)
     */
    fun getLevelProgress(item: ItemStack): Double {
        val counter = itemDataService.getCounter(item)
        val currentLevel = calculateLevelFromCounter(counter, item)

        val currentLevelCounter = calculateCounterForLevel(currentLevel, item)
        val nextLevelCounter = calculateCounterForLevel(currentLevel + 1, item)

        val progressNeeded = nextLevelCounter - currentLevelCounter
        val currentProgress = counter - currentLevelCounter

        return if (progressNeeded > 0) {
            (currentProgress.toDouble() / progressNeeded.toDouble()).coerceIn(0.0, 1.0)
        } else {
            1.0
        }
    }

    /**
     * Calcula quantos pontos o jogador ganha ao subir de nível
     */
    private fun calculatePointsGained(item: ItemStack, fromLevel: Int, toLevel: Int): Int {
        if (toLevel <= MIN_LEVEL || toLevel <= fromLevel) return 0

        val pointsPerLevel = getPointsPerLevel(item)
        var totalPoints = 0

        // Somar pontos para cada nível entre fromLevel e toLevel
        for (level in (fromLevel + 1)..toLevel) {
            totalPoints += pointsPerLevel * (level - 1)
        }

        return totalPoints
    }

    /**
     * Obtém quantos pontos por nível o item dá, baseado no tipo
     */
    private fun getPointsPerLevel(item: ItemStack): Int {
        val evolutionConfig = configManager.getCustomConfig("evolution.yml") ?: return 2
        val pointsSection = evolutionConfig.getConfigurationSection("evolution.points") ?: return 2

        val toolType = ToolType.fromMaterial(item.type)
        val points = when (toolType) {
            ToolType.WOOD -> pointsSection.getInt("wood", 2)
            ToolType.STONE -> pointsSection.getInt("stone", 3)
            ToolType.IRON -> pointsSection.getInt("iron", 4)
            ToolType.DIAMOND -> pointsSection.getInt("diamond", 5)
            ToolType.NETHERITE -> pointsSection.getInt("netherite", 6)
            ToolType.GOLD -> pointsSection.getInt("gold", 6)
            ToolType.LEATHER -> pointsSection.getInt("leather", 2)
            ToolType.CHAINMAIL -> pointsSection.getInt("chainmail", 3)
            null -> when (item.type) {
                Material.BOW -> pointsSection.getInt("bow", 2)
                Material.SHIELD -> pointsSection.getInt("shield", 2)
                Material.CROSSBOW -> pointsSection.getInt("crossbow", 2)
                else -> 0
            }
        }

        return points
    }

    /**
     * Obtém o multiplicador correto (base ou armor) para o item
     */
    private fun getMultiplierForItem(item: ItemStack, settings: EvolutionSettings): Double {
        val category = ToolCategory.fromMaterial(item.type)
        val isArmor = ToolCategory.isArmor(category)
        return if (isArmor) settings.armorMultiplier else settings.baseMultiplier
    }

    /**
     * Verifica se o item é uma ferramenta/armadura válida
     */
    fun isValidTool(item: ItemStack?): Boolean {
        if (item == null) return false
        return ToolType.getAllToolMaterials().contains(item.type)
    }

    /**
     * Retorna o nome da categoria do item (para debug/display)
     */
    fun getToolCategoryName(item: ItemStack?): String {
        if (item == null) return ""

        return when (ToolCategory.fromMaterial(item.type)) {
            ToolCategory.SWORD -> "sword"
            ToolCategory.PICKAXE -> "pickaxe"
            ToolCategory.AXE -> "axe"
            ToolCategory.SHOVEL -> "shovel"
            ToolCategory.HOE -> "hoe"
            ToolCategory.HELMET -> "helmet"
            ToolCategory.CHESTPLATE -> "chestplate"
            ToolCategory.LEGGINGS -> "leggings"
            ToolCategory.BOOTS -> "boots"
            ToolCategory.BOW -> "bow"
            ToolCategory.SHIELD -> "shield"
            ToolCategory.CROSSBOW -> "crossbow"
            else -> ""
        }
    }
}