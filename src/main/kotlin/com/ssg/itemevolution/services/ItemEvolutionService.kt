package com.ssg.itemevolution.services

import com.ssg.itemevolution.core.InitializableService
import com.ssg.itemevolution.core.ReloadableService
import com.ssg.itemevolution.utils.ConfigManager
import com.ssg.itemevolution.keys.ToolCategory
import com.ssg.itemevolution.keys.ToolType
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.math.floor

/**
 * Serviço responsável pela lógica de evolução de itens.
 * Centraliza todos os cálculos relacionados a níveis, pontos e progressão.
 */
class ItemEvolutionService(
    private val configManager: ConfigManager,
    private val itemDataService: ItemDataService,
    private val itemMetaTransferService: ItemMetaTransferService,
    private val materialUpgradeService: MaterialUpgradeService
)  : InitializableService, ReloadableService {

    private val levelUseTable: MutableMap<Material, List<Int>> = mutableMapOf()

    override fun initialize() { generateLevelUseTable() }
    override fun onConfigReload() { generateLevelUseTable() }

    /**
     * Gera a tabela de contadores de uso necessários para cada nível.
     */
    fun generateLevelUseTable() {
        levelUseTable.clear()
        val settings = configManager.getEvolutionSettings()
        val allValidMaterials = ToolType.getAllToolMaterials()

        for (material in allValidMaterials) {
            val durability = material.maxDurability.toDouble()
            val categoryName = ToolCategory.getName(material)

            // Obtém a fórmula customizada (o ConfigManager ainda é responsável por FORNECER a fórmula)
            val formula = configManager.getProgressionFormula(categoryName, material)

            val useList = mutableListOf<Int>()

            for (level in settings.minLevel..settings.maxLevel) {

                val counterCalculated = FormulaHelper.evaluateCounter(
                    formula = formula,
                    level = level,
                    durability = durability
                )

                // O uso real é o valor avaliado, arredondado para o inteiro.
                val counterForLevel = floor(counterCalculated).toInt()

                useList.add(counterForLevel)
            }

            levelUseTable[material] = useList
        }
    }

    /**
     * Obtém a lista de contadores de uso para um material específico.
     * (Antigo getCounterListForMaterial do ConfigManager)
     */
    fun getCounterListForMaterial(material: Material): List<Int>? {
        return levelUseTable[material]
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

    fun calculateLevelFromCounter(counter: Int, item: ItemStack): Int {
        val settings = configManager.getEvolutionSettings()
        val max_level = settings.maxLevel
        val min_level = settings.minLevel

        val counterList = getCounterListForMaterial(item.type)
            ?: return min_level

        val index = counterList.indexOfLast { requiredCounter ->
            counter >= requiredCounter
        }

        return (index + 1).coerceIn(min_level, max_level)
    }

    fun calculateCounterForLevel(level: Int, item: ItemStack): Int {
        val settings = configManager.getEvolutionSettings()
        val max_level = settings.maxLevel
        val min_level = settings.minLevel

        val counterList = getCounterListForMaterial(item.type)
            ?: return 999999999

        val safeLevel = level.coerceIn(min_level, max_level)

        return counterList.getOrNull(safeLevel - 1) ?: counterList.lastOrNull() ?: 0
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
        // Constrói as chaves para buscar na configuração
        val categoryName = getToolCategoryName(item).uppercase()
        val materialName = item.type.name.uppercase()

        // Obtém a lista de pontos customizada
        val pointsList = configManager.getLevelPointsList(categoryName, materialName)

        if (pointsList.isEmpty()) return 0

        var totalPoints = 0

        // Itera pelos NÍVEIS que foram alcançados
        for (level in (fromLevel + 1)..toLevel) {
            // O índice na lista de pontos é sempre Nível - 2
            val listIndex = level - 2

            if (listIndex >= 0 && listIndex < pointsList.size) {
                totalPoints += pointsList[listIndex]
            } else if (listIndex >= pointsList.size) {
                // Parar de ganhar pontos se o nível ultrapassar a configuração
                break
            }
        }

        return totalPoints
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