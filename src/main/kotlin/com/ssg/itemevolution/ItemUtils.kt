package com.ssg.itemevolution

import com.ssg.itemevolution.dialogs.SoulToolUtils.hasSoul
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.persistence.PersistentDataContainer
import kotlin.math.*

object ItemUtils {

    fun testTool(item: ItemStack?): String {
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

    fun isValidTool(item: ItemStack?): Boolean {
        if (item == null) return false
        return ToolType.getAllToolMaterials().contains(item.type)
    }

    fun getPointsUp(item: ItemStack, level: Int): Int {
        var up = 0

        if (level <= 5) {
            val toolType = ToolType.fromMaterial(item.type)
            up = when (toolType) {
                ToolType.WOOD, ToolType.LEATHER -> 2
                ToolType.GOLD -> 6
                ToolType.STONE, ToolType.CHAINMAIL -> 3
                ToolType.IRON -> 4
                ToolType.DIAMOND -> 5
                ToolType.NETHERITE -> 6
                null -> when (item.type) {
                    Material.BOW, Material.SHIELD, Material.CROSSBOW -> 2
                    else -> 0
                }
            }
        }

        return up * (level - 1)
    }

    fun setupItem(item: ItemStack): ItemStack {
        val meta = item.itemMeta ?: return item
        val container = meta.persistentDataContainer
        container.set(EvolutionKey.USES.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER, 0)
        container.set(EvolutionKey.COUNTER.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER, 1)
        container.set(EvolutionKey.LEVEL.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER, 1)
        container.set(EvolutionKey.POINTS.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER, 0)
        item.itemMeta = meta
        return item
    }

    fun upgradeItem(item: ItemStack): ItemStack {
        if (!hasSoul(item)) return item
        val meta = item.itemMeta ?: return item
        val container = meta.persistentDataContainer
        val isArmor = ToolCategory.isArmor(ToolCategory.fromMaterial(item.type))
        val mult = if (isArmor) 0.25 else 1.0

        val currentUses = container.get(EvolutionKey.USES.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER) ?: 0
        val newUses = currentUses + 1
        val counter = (container.get(EvolutionKey.COUNTER.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER) ?: 0) + 1

        container.set(EvolutionKey.COUNTER.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER, counter)
        container.set(EvolutionKey.USES.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER, newUses)

        val durability = item.type.maxDurability.toDouble()
        val newLevel = floor(((counter / (durability * mult)).pow(0.75)) + 1).toInt()

        val currentLevel = container.get(EvolutionKey.LEVEL.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER) ?: 1

        if (newLevel > currentLevel) {
            val pointsUp = getPointsUp(item, newLevel)
            val currentPoints = container.get(EvolutionKey.POINTS.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER) ?: 0
            val newPoints = currentPoints + pointsUp

            container.set(EvolutionKey.LEVEL.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER, newLevel)
            container.set(EvolutionKey.POINTS.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER, newPoints)
        }

        item.itemMeta = meta
        return item
    }

    fun improveItem(item: ItemStack): ItemStack {
        ToolType.fromMaterial(item.type) ?: return item

        // Encontrar o próximo material da mesma categoria
        val nextMaterial = getUpgradeMaterial(item.type) ?: return item
        val newItem = ItemStack(nextMaterial, item.amount)

        val meta = newItem.itemMeta ?: return newItem
        val container = meta.persistentDataContainer

        // Transferir dados do item antigo
        val oldMeta = item.itemMeta
        if (oldMeta != null) {
            val oldContainer = oldMeta.persistentDataContainer

            // Copiar dados persistentes (sem incluir encantamentos customizados antigos)
            copyEvolutionData(oldContainer, container)

            // Copiar outros dados do meta (display name, etc.)
            meta.displayName(oldMeta.displayName())

            if (oldMeta.hasCustomModelData()) {
                meta.setCustomModelData(oldMeta.customModelData)
            }

            meta.isUnbreakable = oldMeta.isUnbreakable
            meta.addItemFlags(*oldMeta.itemFlags.toTypedArray())
            oldMeta.attributeModifiers?.let { meta.attributeModifiers = it }
        }

        // Reset para level 1
        container.set(EvolutionKey.COUNTER.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER, 1)
        container.set(EvolutionKey.LEVEL.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER, 1)

        // Definir o meta ANTES de copiar encantamentos
        newItem.itemMeta = meta

        // Copiar todos os encantamentos (vanilla e data-driven)
        if (item.enchantments.isNotEmpty()) {
            newItem.addUnsafeEnchantments(item.enchantments)
        }

        return newItem
    }

    /**
     * Copia apenas os dados de evolução necessários
     */
    private fun copyEvolutionData(source: PersistentDataContainer, target: PersistentDataContainer) {
        val evolutionKeys = listOf(
            EvolutionKey.USES,
            EvolutionKey.COUNTER,
            EvolutionKey.LEVEL,
            EvolutionKey.POINTS
        )

        for (evolutionKey in evolutionKeys) {
            val key = evolutionKey.key(ItemEvolutionPlugin.instance)
            val value = source.get(key, PersistentDataType.INTEGER)
            if (value != null) {
                target.set(key, PersistentDataType.INTEGER, value)
            }
        }
    }

    private fun getUpgradeMaterial(current: Material): Material? {
        val category = ToolCategory.fromMaterial(current)
        val currentType = ToolType.fromMaterial(current)

        return when (currentType) {
            ToolType.WOOD -> getNextMaterial(ToolType.STONE, category)
            ToolType.STONE -> getNextMaterial(ToolType.IRON, category)
            ToolType.IRON -> getNextMaterial(ToolType.DIAMOND, category)
            ToolType.DIAMOND -> getNextMaterial(ToolType.NETHERITE, category)
            ToolType.LEATHER -> getNextMaterial(ToolType.IRON, category) // Pula chain
            ToolType.CHAINMAIL -> getNextMaterial(ToolType.IRON, category)
            else -> null
        }
    }

    private fun getNextMaterial(toolType: ToolType, category: ToolCategory): Material? {
        return toolType.materials.find { ToolCategory.fromMaterial(it) == category }
    }

    /**
     * Retorna a descrição do item (anteriormente era updateLore)
     * Para ser usado em futuras GUIs
     */
    fun getDescription(item: ItemStack): List<Component> {
        val meta = item.itemMeta ?: return emptyList()
        val container = meta.persistentDataContainer

        val description = mutableListOf<Component>()

        // Listar encantamentos do item
        val enchantments = item.enchantments
        if (enchantments.isNotEmpty()) {
            for ((enchantment, level) in enchantments) {
                val enchantName = enchantment.key.key.replaceFirstChar { it.uppercase() }
                description.add(Component.text("§7$enchantName ${toRoman(level)}"))
            }
            description.add(Component.text("§7----------------------"))
        }

        // Estatísticas de evolução
        val uses = container.get(EvolutionKey.USES.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER) ?: 0
        val level = container.get(EvolutionKey.LEVEL.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER) ?: 1
        val points = container.get(EvolutionKey.POINTS.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER) ?: 0
        val counter = container.get(EvolutionKey.COUNTER.key(ItemEvolutionPlugin.instance), PersistentDataType.INTEGER) ?: 1

        val durability = item.type.maxDurability.toDouble()
        val isArmor = ToolCategory.isArmor(ToolCategory.fromMaterial(item.type))
        val mult = if (isArmor) 0.25 else 1.0

        val before = ceil(((durability * mult).pow(3) * (level - 1).toDouble().pow(4)).pow(1.0 / 3.0)).toInt()
        val nextLvl = ceil(((durability * mult).pow(3) * level.toDouble().pow(4)).pow(1.0 / 3.0) - before).toInt()
        val toNext = counter - before

        description.add(Component.text("§7Usos: $uses"))
        description.add(Component.text("§7Lvl: $level"))
        description.add(Component.text("§7Pontos: $points"))
        description.add(Component.text("§7NextLvl: $toNext/$nextLvl"))
        description.add(Component.text("§7----------------------"))

        return description
    }

    private fun toRoman(number: Int): String {
        return when (number) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            6 -> "VI"
            7 -> "VII"
            8 -> "VIII"
            9 -> "IX"
            10 -> "X"
            else -> number.toString()
        }
    }

    fun getRepairCost(item: ItemStack): Int {
        val meta = item.itemMeta as? Damageable ?: return 0
        val damage = meta.damage
        val maxDurability = item.type.maxDurability
        return ceil(3.0 * damage / maxDurability).toInt()
    }

    fun getRepairMaterial(item: ItemStack): Material? {
        val toolType = ToolType.fromMaterial(item.type)
        return when (toolType) {
            ToolType.WOOD -> Material.OAK_PLANKS
            ToolType.STONE -> Material.COBBLESTONE
            ToolType.IRON -> Material.IRON_INGOT
            ToolType.DIAMOND -> Material.DIAMOND
            ToolType.NETHERITE -> Material.NETHERITE_SCRAP
            ToolType.LEATHER -> Material.LEATHER
            ToolType.CHAINMAIL -> Material.IRON_NUGGET
            ToolType.GOLD -> Material.GOLD_INGOT
            null -> when (item.type) {
                Material.BOW -> Material.STICK
                else -> null
            }
        }
    }

    fun getUpgradeMaterial(item: ItemStack): Material? {
        val toolType = ToolType.fromMaterial(item.type)
        return when (toolType) {
            ToolType.WOOD -> Material.STONE
            ToolType.STONE -> Material.IRON_INGOT
            ToolType.IRON -> Material.DIAMOND
            ToolType.DIAMOND -> Material.NETHERITE_INGOT
            ToolType.LEATHER -> Material.IRON_NUGGET
            ToolType.CHAINMAIL -> Material.IRON_INGOT
            else -> null
        }
    }

    fun getMaterialQuantity(material: Material): Int {
        return when (ToolCategory.fromMaterial(material)) {
            ToolCategory.SWORD -> 2
            ToolCategory.PICKAXE -> 3
            ToolCategory.AXE -> 3
            ToolCategory.SHOVEL -> 1
            ToolCategory.HOE -> 1
            ToolCategory.HELMET -> 5
            ToolCategory.CHESTPLATE -> 8
            ToolCategory.LEGGINGS -> 7
            ToolCategory.BOOTS -> 4
            ToolCategory.BOW -> 2
            ToolCategory.SHIELD -> 2
            ToolCategory.CROSSBOW -> 2
            else -> 1
        }
    }

    fun getUpgradeItemstack(item: ItemStack): ItemStack? {
        val upgradeMaterial = getUpgradeMaterial(item) ?: return null
        val upgradeAmount = getMaterialQuantity(upgradeMaterial)
        if (upgradeMaterial == Material.NETHERITE_INGOT) return ItemStack(Material.NETHERITE_INGOT, 1)
        return ItemStack(upgradeMaterial, upgradeAmount)
    }
}