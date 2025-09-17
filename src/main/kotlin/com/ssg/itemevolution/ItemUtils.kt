package com.ssg.itemevolution

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import kotlin.math.*

object ItemUtils {

    private val counterKey = NamespacedKey(ItemEvolutionPlugin.instance, "fplus_contador")
    private val levelKey = NamespacedKey(ItemEvolutionPlugin.instance, "fplus_level")
    private val pointsKey = NamespacedKey(ItemEvolutionPlugin.instance, "fplus_pontos")
    private val usesKey = NamespacedKey(ItemEvolutionPlugin.instance, "fplus_usos")
    private val encKey = NamespacedKey(ItemEvolutionPlugin.instance, "fplus_enc")
    private val customEnchantmentsKey = NamespacedKey(ItemEvolutionPlugin.instance, "custom_enc")
    private val customEnchantmentLevelsKey = NamespacedKey(ItemEvolutionPlugin.instance, "custom_enc_lvl")
    private val customEnchantmentDisplayKey = NamespacedKey(ItemEvolutionPlugin.instance, "custom_enc_display")

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

    fun upgradeItem(item: ItemStack): ItemStack {
        val meta = item.itemMeta ?: return item
        val container = meta.persistentDataContainer

        val isArmor = ToolCategory.isArmor(ToolCategory.fromMaterial(item.type))
        val mult = if (isArmor) 0.25 else 1.0

        val currentUses = container.get(usesKey, PersistentDataType.INTEGER) ?: 0

        if (currentUses == 0) {
            // Primeiro uso
            container.set(counterKey, PersistentDataType.INTEGER, 1)
            container.set(levelKey, PersistentDataType.INTEGER, 1)
            container.set(pointsKey, PersistentDataType.INTEGER, 0)
            container.set(usesKey, PersistentDataType.INTEGER, 1)
        } else {
            // Incrementar usos
            val newUses = currentUses + 1
            val counter = (container.get(counterKey, PersistentDataType.INTEGER) ?: 0) + 1

            container.set(counterKey, PersistentDataType.INTEGER, counter)
            container.set(usesKey, PersistentDataType.INTEGER, newUses)
        }

        val durability = item.type.maxDurability.toDouble()
        val counter = container.get(counterKey, PersistentDataType.INTEGER) ?: 1
        val newLevel = floor(((counter / (durability * mult)).pow(0.75)) + 1).toInt()

        val currentLevel = container.get(levelKey, PersistentDataType.INTEGER) ?: 1

        if (newLevel > currentLevel) {
            val pointsUp = getPointsUp(item, newLevel)
            val currentPoints = container.get(pointsKey, PersistentDataType.INTEGER) ?: 0
            val newPoints = currentPoints + pointsUp

            container.set(levelKey, PersistentDataType.INTEGER, newLevel)
            container.set(pointsKey, PersistentDataType.INTEGER, newPoints)
        }

        item.itemMeta = meta
        updateLore(item)
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

            // ✅ CORREÇÃO: Copiar TODOS os dados persistentes, incluindo encantamentos customizados
            copyAllPersistentData(oldContainer, container)

            // Copiar outros dados do meta (display name, lore customizado, etc.)
            meta.displayName(oldMeta.displayName())
            // ❌ NÃO copiar lore antigo - será recriado pelo updateLore()
            // meta.lore(oldMeta.lore())

            if (oldMeta.hasCustomModelData()) { meta.setCustomModelData(oldMeta.customModelData) }

            meta.isUnbreakable = oldMeta.isUnbreakable
            meta.addItemFlags(*oldMeta.itemFlags.toTypedArray())
            oldMeta.attributeModifiers?.let { meta.attributeModifiers = it }
        }

        // Reset para level 1 (sobrescrever apenas estes valores específicos)
        container.set(counterKey, PersistentDataType.INTEGER, 1)
        container.set(levelKey, PersistentDataType.INTEGER, 1)

        // Definir o meta ANTES de copiar encantamentos vanilla
        newItem.itemMeta = meta

        // DEPOIS copiar encantamentos vanilla
        if (item.enchantments.isNotEmpty()) {
            newItem.addUnsafeEnchantments(item.enchantments)
        }

        updateLore(newItem)
        return newItem
    }

    /**
     * Copia TODOS os dados persistentes de um container para outro
     */
    private fun copyAllPersistentData(source: PersistentDataContainer, target: PersistentDataContainer) {
        for (key in source.keys) {
            when {
                source.has(key, PersistentDataType.STRING) -> {
                    val value = source.get(key, PersistentDataType.STRING)
                    if (value != null) target.set(key, PersistentDataType.STRING, value)
                }
                source.has(key, PersistentDataType.INTEGER) -> {
                    val value = source.get(key, PersistentDataType.INTEGER)
                    if (value != null) target.set(key, PersistentDataType.INTEGER, value)
                }
                source.has(key, PersistentDataType.DOUBLE) -> {
                    val value = source.get(key, PersistentDataType.DOUBLE)
                    if (value != null) target.set(key, PersistentDataType.DOUBLE, value)
                }
                source.has(key, PersistentDataType.LONG) -> {
                    val value = source.get(key, PersistentDataType.LONG)
                    if (value != null) target.set(key, PersistentDataType.LONG, value)
                }
                source.has(key, PersistentDataType.BYTE) -> {
                    val value = source.get(key, PersistentDataType.BYTE)
                    if (value != null) target.set(key, PersistentDataType.BYTE, value)
                }
                source.has(key, PersistentDataType.FLOAT) -> {
                    val value = source.get(key, PersistentDataType.FLOAT)
                    if (value != null) target.set(key, PersistentDataType.FLOAT, value)
                }
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

    fun updateLore(item: ItemStack) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer

        // Criar a lista de lore como Component
        val lore = mutableListOf<Component>()

        // Enchantments customizados
        val customEnchantments = container.get(customEnchantmentsKey, PersistentDataType.STRING)?.split(",") ?: emptyList()
        val customLevels = container.get(customEnchantmentLevelsKey, PersistentDataType.STRING)?.split(",") ?: emptyList()
        val displayFlags = container.get(customEnchantmentDisplayKey, PersistentDataType.STRING)?.split(",") ?: emptyList()

        for (i in customEnchantments.indices) {
            if (i < customLevels.size) {
                if (displayFlags[i].toBooleanStrictOrNull() == false) continue
                val enchantName = customEnchantments[i]
                val level = customLevels[i].toIntOrNull() ?: 1
                lore.add(Component.text("§7$enchantName ${toRoman(level)}"))
            }
        }

        // Separador
        lore.add(Component.text("§7----------------------"))

        // Estatísticas
        val uses = container.get(usesKey, PersistentDataType.INTEGER) ?: 0
        lore.add(Component.text("§7Usos: $uses"))

        val encValue = container.get(encKey, PersistentDataType.INTEGER) ?: 0
        if (encValue != 1) {
            val level = container.get(levelKey, PersistentDataType.INTEGER) ?: 1
            val points = container.get(pointsKey, PersistentDataType.INTEGER) ?: 0
            val counter = container.get(counterKey, PersistentDataType.INTEGER) ?: 1

            val durability = item.type.maxDurability.toDouble()
            val isArmor = ToolCategory.isArmor(ToolCategory.fromMaterial(item.type))
            val mult = if (isArmor) 0.25 else 1.0

            val before = ceil(((durability * mult).pow(3) * (level - 1).toDouble().pow(4)).pow(1.0 / 3.0)).toInt()
            val nextLvl = ceil(((durability * mult).pow(3) * level.toDouble().pow(4)).pow(1.0 / 3.0) - before).toInt()
            val toNext = counter - before

            lore.add(Component.text("§7Lvl: $level"))
            lore.add(Component.text("§7Pontos: $points"))
            lore.add(Component.text("§7NextLvl: $toNext/$nextLvl"))
        }

        lore.add(Component.text("§7----------------------"))

        // Definir a lore usando Adventure Components
        meta.lore(lore)
        item.itemMeta = meta
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
}