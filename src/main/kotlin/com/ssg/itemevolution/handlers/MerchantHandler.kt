package com.ssg.itemevolution.handlers

import com.nexomc.nexo.api.NexoItems
import com.ssg.itemevolution.ItemEvolutionPlugin
import com.ssg.itemevolution.utils.ConfigManager
import com.ssg.itemevolution.enchantments.utility.EternaEnchantment.Companion.isItemBroken
import com.ssg.itemevolution.services.EnchantmentService
import com.ssg.itemevolution.services.ItemEvolutionService
import com.ssg.itemevolution.services.ItemRepairService
import com.ssg.itemevolution.services.MaterialUpgradeService
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
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.persistence.PersistentDataType

class MerchantHandler(
    private val plugin: ItemEvolutionPlugin,
    private val configManager: ConfigManager,
    private val enchantmentService: EnchantmentService,
    private val itemEvolutionService: ItemEvolutionService,
    private val itemRepairService: ItemRepairService,
    private val materialUpgradeService: MaterialUpgradeService,
    private val visualEnchantmentService: VisualEnchantmentService
) {
    fun openMerchant(player: Player, tool: ItemStack) {
        if (!itemEvolutionService.isValidTool(tool)) {
            player.sendMessage("§4[SSG] §2Você precisa usar uma ferramenta válida na mão para acessar isso")
            return
        }

        val trades = mutableListOf<MerchantRecipe>()
        val isItemBroken = isItemBroken(tool)
        addRepairTrade(tool, trades)

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

    private fun addRepairTrade(tool: ItemStack, trades: MutableList<MerchantRecipe>) {
        if (!itemRepairService.canRepair(tool)) return

        val repairItems = itemRepairService.getRepairItemStack(tool) ?: return
        val repairedTool = itemRepairService.createRepairedItem(tool)

        val repairRecipe = MerchantRecipe(repairedTool, 999)
        repairRecipe.addIngredient(tool)
        repairRecipe.addIngredient(repairItems)
        trades.add(repairRecipe)
    }

    private fun addUpgradeTrades(tool: ItemStack, trades: MutableList<MerchantRecipe>) {
        val upgradeItemStack = materialUpgradeService.getUpgradeItemStack(tool)
        if (upgradeItemStack != null) {
            val upgradedTool = itemEvolutionService.improveItem(tool)
            visualEnchantmentService.updateVisualModelOnUpgrade(tool, upgradedTool)
            val upgradeRecipe = MerchantRecipe(upgradedTool, 999)
            upgradeRecipe.addIngredient(tool)
            upgradeRecipe.addIngredient(upgradeItemStack)
            trades.add(upgradeRecipe)
        }
    }

    private fun addEnchantmentTrades(tool: ItemStack, trades: MutableList<MerchantRecipe>) {
        val config = configManager.getCustomConfig("enchantments.yml") ?: return
        val toolType = itemEvolutionService.getToolCategoryName(tool)

        if (!config.contains("enchantments")) return

        val enchantments = config.getConfigurationSection("enchantments")?.getKeys(false) ?: return
        val allowedEnchantments = config.getStringList("tool-compatibility.$toolType")

        for (enchantName in enchantments) {
            if (!allowedEnchantments.contains(enchantName)) continue
            createEnchantmentTrade(tool, enchantName, config, trades)
        }
    }

    private fun addTrade(
        result: ItemStack,
        tool: ItemStack,
        cost: ItemStack,
        maxUses: Int,
        trades: MutableList<MerchantRecipe>
    ) {
        val recipe = MerchantRecipe(result, maxUses)
        recipe.addIngredient(tool)
        recipe.addIngredient(cost)
        trades.add(recipe)
    }


    private fun createEnchantmentTrade(
        tool: ItemStack,
        enchantName: String,
        config: FileConfiguration,
        trades: MutableList<MerchantRecipe>,
    ) {
        val enchantSection = config.getConfigurationSection("enchantments.$enchantName") ?: return
        val costItems = enchantSection.getStringList("item-costs")
        val costPoints = enchantSection.getIntegerList("point-costs")

        val currentLevel = enchantmentService.getEnchantmentLevel(tool, enchantName)
        val nextLevel = currentLevel + 1

        val rawCostString = costItems.getOrNull(currentLevel) ?: return
        val cost = parseOrCreateCostItem(rawCostString)

        // 1. Cria o item de pré-visualização/resultado (independente de estar bloqueado)
        val enchantedTool = enchantmentService.enchantItem(tool, enchantName, nextLevel)

        // 2. Aplica o modelo visual ao item de pré-visualização
        if (visualEnchantmentService.hasVisualModel(enchantName)) {
            visualEnchantmentService.applyVisualModel(enchantedTool, enchantName)
        }

        // A. Bloqueio por Incompatibilidade
        if (!enchantmentService.isEnchantmentCompatible(tool, enchantName)) { // Verifica e entra
            val conflicting = enchantmentService.getConflictingEnchantments(enchantName)
            val conflictNames = conflicting.joinToString(", ") { it.replace("_", " ").capitalize() }

            val lore = listOf(
                Component.text("⚠ Incompatível com:").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                Component.text(conflictNames).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            )

            addBlockedTrade(enchantedTool, tool, cost, trades, lore)
            return // Trade bloqueado adicionado, sai da função
        }

        // B. Bloqueio por Pontos Insuficientes
        val requiredPoints = costPoints.getOrNull(currentLevel) ?: return
        val currentPoints = getCurrentPoints(tool)

        if (requiredPoints > currentPoints) { // Verifica e entra
            val lore = listOf(
                Component.text("Pontos insuficientes: $currentPoints/$requiredPoints")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
            )

            addBlockedTrade(enchantedTool, tool, cost, trades, lore)
            return // Trade bloqueado adicionado, sai da função
        }

        // --- Trade Válido ---
        // Reduz os pontos SOMENTE no item final
        reducePoints(enchantedTool, requiredPoints)

        // Adiciona o trade normal (maxUses = 1)
        addTrade(enchantedTool, tool, cost, 1, trades)
    }
    /**
     * Prepara o item de custo com o visual de bloqueio e adiciona o trade com 0 usos.
     */
    private fun addBlockedTrade(
        previewTool: ItemStack,
        tool: ItemStack,
        cost: ItemStack,
        trades: MutableList<MerchantRecipe>,
        loreLines: List<Component>
    ) {
        // Prepara o item de custo com o visual de bloqueio (encantamento, flags, lore)
        cost.addUnsafeEnchantment(Enchantment.UNBREAKING, 10)
        val meta = cost.itemMeta
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        meta.lore(loreLines)
        cost.itemMeta = meta

        // Adiciona o trade bloqueado (maxUses = 0)
        addTrade(previewTool, tool, cost, 0, trades)
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
}