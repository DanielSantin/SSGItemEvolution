package com.ssg.itemevolution.handlers

import com.nexomc.nexo.api.NexoItems
import com.ssg.itemevolution.ItemEvolutionPlugin
import com.ssg.itemevolution.utils.ItemUtils
import com.ssg.itemevolution.utils.ConfigManager
import com.ssg.itemevolution.enchantments.utility.EternaEnchantment.Companion.isItemBroken
import com.ssg.itemevolution.enchantments.utility.EternaEnchantment.Companion.removeBrokenItemMark
import com.ssg.itemevolution.services.EnchantmentService
import com.ssg.itemevolution.services.VisualEnchantmentService
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.persistence.PersistentDataType

class MerchantHandler(
    private val plugin: ItemEvolutionPlugin,
    private val itemUtils: ItemUtils,
    private val configManager: ConfigManager,
    private val enchantmentService: EnchantmentService,
    private val visualEnchantmentService: VisualEnchantmentService
) {
    private val toolItemKey = NamespacedKey(plugin, "tool_item")


    fun openMerchant(player: Player, tool: ItemStack) {
        if (!itemUtils.isValidTool(tool)) {
            player.sendMessage("§4[SSG] §2Você precisa usar uma ferramenta válida na mão para acessar isso")
            return
        }

        val trades = mutableListOf<MerchantRecipe>()
        val isItemBroken = isItemBroken(tool)
        val repairCostMultiplier = if (isItemBroken) 2 else 1
        addRepairTrade(tool, trades, repairCostMultiplier)

        if(!isItemBroken){
            addUpgradeTrades(tool, trades)
            addEnchantmentTrades(tool, trades)
        }

        if (trades.isEmpty()) {
            player.sendMessage("§4[SSG] §2Nenhuma melhoria disponível para este item")
            return
        }

        // Carregar merchant.yml e pegar título
        val merchantConfig = configManager.getCustomConfig("merchant.yml")
        val title = merchantConfig?.getString("merchant.title") ?: "Mercador"

        val merchant = Bukkit.createMerchant(Component.text(title))
        merchant.recipes = trades

        player.openMerchant(merchant, true)
        player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 2.0f)
    }

    private fun addRepairTrade(tool: ItemStack, trades: MutableList<MerchantRecipe>, repairCostMultiplier: Int = 1) {
        val repairMaterial = itemUtils.getRepairMaterial(tool)
        if (repairMaterial != null) {
            val meta = tool.itemMeta as? Damageable
            if (meta != null && meta.damage > 0) {
                val repairCost = itemUtils.getRepairCost(tool) * repairCostMultiplier
                val repairItems = ItemStack(repairMaterial, repairCost)
                val repairedTool = tool.clone()
                removeBrokenItemMark(repairedTool)
                val repairedMeta = repairedTool.itemMeta as Damageable
                repairedMeta.damage = 0
                repairedTool.itemMeta = repairedMeta

                val repairRecipe = MerchantRecipe(repairedTool, 999)
                repairRecipe.addIngredient(tool)
                repairRecipe.addIngredient(repairItems)
                trades.add(repairRecipe)
            }
        }
    }

    private fun addUpgradeTrades(tool: ItemStack, trades: MutableList<MerchantRecipe>) {
        val upgradeItemStack = itemUtils.getUpgradeItemstack(tool)
        if (upgradeItemStack != null) {
            val upgradedTool = itemUtils.improveItem(tool)

            // 🔧 ATUALIZAR MODELO VISUAL ao fazer upgrade
            visualEnchantmentService.updateVisualModelOnUpgrade(tool, upgradedTool)

            val upgradeRecipe = MerchantRecipe(upgradedTool, 999)
            upgradeRecipe.addIngredient(tool)
            upgradeRecipe.addIngredient(upgradeItemStack)
            trades.add(upgradeRecipe)
        }
    }

    private fun addEnchantmentTrades(tool: ItemStack, trades: MutableList<MerchantRecipe>) {
        val config = configManager.getCustomConfig("enchantments.yml") ?: return
        val toolType = itemUtils.testTool(tool)

        if (!config.contains("enchantments")) return

        val enchantments = config.getConfigurationSection("enchantments")?.getKeys(false) ?: return
        val allowedEnchantments = config.getStringList("tool-compatibility.$toolType")

        for (enchantName in enchantments) {
            if (!allowedEnchantments.contains(enchantName)) continue

            // Se for incompatível → trade bloqueada
            if (!enchantmentService.isEnchantmentCompatible(tool, enchantName)) {
                createBlockedTrade(tool, enchantName, config, trades)
                continue
            }

            // Se for compatível → trade normal
            createEnchantmentTrade(tool, enchantName, config, trades)
        }
    }


    private fun createBlockedTrade(
        tool: ItemStack,
        enchantName: String,
        config: FileConfiguration,
        trades: MutableList<MerchantRecipe>
    ) {
        val enchantSection = config.getConfigurationSection("enchantments.$enchantName") ?: return
        val costItems = enchantSection.getStringList("item-costs")
        val currentLevel = enchantmentService.getEnchantmentLevel(tool, enchantName)
        val rawCostString = costItems.getOrNull(currentLevel) ?: return
        val cost = parseOrCreateCostItem(rawCostString)

        // Marcar como item bloqueado
        cost.addUnsafeEnchantment(Enchantment.UNBREAKING, 10)
        val meta = cost.itemMeta
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

        val conflicting = enchantmentService.getConflictingEnchantments(enchantName)
        val conflictNames = conflicting.joinToString(", ") { it.replace("_", " ").capitalize() }

        meta.lore(listOf(
            Component.text("⚠ Incompatível com:").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
            Component.text(conflictNames).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ))
        cost.itemMeta = meta

        val blockedTool =  enchantmentService.enchantItem(tool.clone(), enchantName, 1)

        val recipe = MerchantRecipe(blockedTool, 0) // maxUses = 0
        recipe.addIngredient(tool)
        recipe.addIngredient(cost)
        trades.add(recipe)
    }


    private fun createEnchantmentTrade(
        tool: ItemStack,
        enchantName: String,
        config: FileConfiguration,
        trades: MutableList<MerchantRecipe>
    ) {
        val enchantSection = config.getConfigurationSection("enchantments.$enchantName") ?: return
        val costPoints = enchantSection.getIntegerList("point-costs")
        val costItems = enchantSection.getStringList("item-costs")

        val currentLevel = enchantmentService.getEnchantmentLevel(tool, enchantName)
        val nextLevel = currentLevel + 1
        if (nextLevel > costPoints.size) return

        val requiredPoints = costPoints.getOrNull(currentLevel) ?: return
        val currentPoints = getCurrentPoints(tool)
        val rawCostString = costItems.getOrNull(currentLevel) ?: return
        val cost = parseOrCreateCostItem(rawCostString)

        // Se pontos insuficientes → trade "cinza"
        if (requiredPoints > currentPoints) {
            makeInsufficientPointsTrade(tool, enchantName, cost, trades, requiredPoints, currentPoints)
            return
        }

        // Aplicar encantamento de verdade
        val enchantedTool = enchantmentService.enchantItem(tool, enchantName, nextLevel)
        reducePoints(enchantedTool, requiredPoints)

        if (visualEnchantmentService.hasVisualModel(enchantName)) {
            visualEnchantmentService.applyVisualModel(enchantedTool, enchantName)
        }

        val recipe = MerchantRecipe(enchantedTool, 1)
        recipe.addIngredient(tool)
        recipe.addIngredient(cost)
        trades.add(recipe)
    }


    private fun makeInsufficientPointsTrade(
        tool: ItemStack,
        enchantName: String,
        cost: ItemStack,
        trades: MutableList<MerchantRecipe>,
        required: Int,
        current: Int
    ) {
        cost.addUnsafeEnchantment(Enchantment.UNBREAKING, 10)
        val meta = cost.itemMeta
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        meta.lore(listOf(
            Component.text("Pontos insuficientes: $current/$required")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
        ))
        cost.itemMeta = meta

        val previewTool = enchantmentService.enchantItem(tool, enchantName, enchantmentService.getEnchantmentLevel(tool, enchantName) + 1)

        val recipe = MerchantRecipe(previewTool, 0) // bloqueado
        recipe.addIngredient(tool)
        recipe.addIngredient(cost)
        trades.add(recipe)
    }


    /**
     * Auxiliar para processar o custo do item
     */
    private fun parseOrCreateCostItem(rawCostString: String): ItemStack {
        val trimmed = rawCostString.trim()
        val qtyRegex = Regex("^([0-9]+)x?\\s+(.+)$")
        val match = qtyRegex.find(trimmed)

        val quantity = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val itemSpec = match?.groupValues?.get(2) ?: trimmed

        return if (itemSpec.startsWith("nexo:")) {
            val itemId = itemSpec.removePrefix("nexo:").trim()
            try {
                val itemBuilder = NexoItems.itemFromId(itemId)
                if (itemBuilder == null) {
                    plugin.logger.warning("ID do Nexo inválido: $itemId")
                    ItemStack(Material.BARRIER).apply {
                        val meta = itemMeta
                        meta.displayName(Component.text("Item Inválido").color(NamedTextColor.RED))
                        itemMeta = meta
                    }
                } else {
                    val itemStack = itemBuilder.build()
                    itemStack.amount = quantity.coerceAtLeast(1).coerceAtMost(itemStack.maxStackSize)
                    itemStack
                }
            } catch (e: Exception) {
                plugin.logger.warning("Erro ao criar item Nexo ($itemId): ${e.message}")
                ItemStack(Material.BARRIER).apply {
                    val meta = itemMeta
                    meta.displayName(Component.text("Erro ao Carregar").color(NamedTextColor.RED))
                    itemMeta = meta
                }
            }
        } else {
            parseItemStack(rawCostString)
        }
    }

    private fun getCurrentPoints(tool: ItemStack): Int {
        val meta = tool.itemMeta ?: return 0
        val container = meta.persistentDataContainer
        val pointsKey = NamespacedKey(plugin, "fplus_pontos")
        return container.get(pointsKey, PersistentDataType.INTEGER) ?: 0
    }

    private fun reducePoints(tool: ItemStack, points: Int) {
        val meta = tool.itemMeta ?: return
        val container = meta.persistentDataContainer
        val pointsKey = NamespacedKey(plugin, "fplus_pontos")

        val currentPoints = container.get(pointsKey, PersistentDataType.INTEGER) ?: 0
        container.set(pointsKey, PersistentDataType.INTEGER, maxOf(0, currentPoints - points))
        tool.itemMeta = meta
    }

    fun parseItemStack(raw: String): ItemStack {
        val parts = raw.split(" ")
        val amount = parts[0].toIntOrNull() ?: 1
        val materialName = parts[1].uppercase()
        val mat = Material.getMaterial(materialName) ?: return ItemStack(Material.STONE)

        val item = ItemStack(mat, amount)

        if (mat == Material.ENCHANTED_BOOK && parts.size >= 4) {
            val enchantName = parts[2].lowercase()
            val level = parts[3].toIntOrNull() ?: 1
            val meta = item.itemMeta as EnchantmentStorageMeta

            val key = NamespacedKey.minecraft(enchantName)

            val enchant = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(key)

            if (enchant != null) {
                meta.addStoredEnchant(enchant, level, true)
                item.itemMeta = meta
            } else {
                println("Encantamento '$enchantName' não encontrado!")
            }
        }

        return item
    }

    private fun deserializeItemStack(data: String): ItemStack? {
        val parts = data.split(":")
        if (parts.size != 2) return null

        val material = Material.getMaterial(parts[0]) ?: return null
        val amount = parts[1].toIntOrNull() ?: 1

        return ItemStack(material, amount)
    }

    fun restoreToolItem(player: Player) {
        val meta = player.persistentDataContainer
        val toolData = meta.get(toolItemKey, PersistentDataType.STRING)

        if (toolData != null) {
            val tool = deserializeItemStack(toolData)
            if (tool != null) {
                player.inventory.addItem(tool)
            }
            meta.remove(toolItemKey)
        }
    }
}