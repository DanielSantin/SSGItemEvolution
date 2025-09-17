package com.ssg.itemevolution

import com.nexomc.nexo.api.NexoItems
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import org.bukkit.inventory.meta.BannerMeta
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.DyeColor
import org.bukkit.block.banner.Pattern
import org.bukkit.block.banner.PatternType
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.EnchantmentStorageMeta

object MerchantHandler {

    private val toolItemKey = NamespacedKey(ItemEvolutionPlugin.instance, "tool_item")

    fun openMerchant(player: Player, tool: ItemStack) {
        if (!ItemUtils.isValidTool(tool)) {
            player.sendMessage("§4[SSG] §2Você precisa usar uma ferramenta válida na mão para acessar isso")
            return
        }

        val trades = mutableListOf<MerchantRecipe>()

        // Trade de upgrade
        val upgradeMaterial = ItemUtils.getUpgradeMaterial(tool)
        if (upgradeMaterial != null) {
            val quantity = MaterialQuantity.getMaterialQuantity(tool.type)
            val upgradeItems = ItemStack(upgradeMaterial, quantity)
            val upgradedTool = ItemUtils.improveItem(tool)

            val upgradeRecipe = MerchantRecipe(upgradedTool, 999)
            upgradeRecipe.addIngredient(tool)
            upgradeRecipe.addIngredient(upgradeItems)
            trades.add(upgradeRecipe)
        }

        // Trade de reparo
        val repairMaterial = ItemUtils.getRepairMaterial(tool)
        if (repairMaterial != null) {
            val meta = tool.itemMeta as? Damageable
            if (meta != null && meta.damage > 0) {
                val repairCost = ItemUtils.getRepairCost(tool)
                val repairItems = ItemStack(repairMaterial, repairCost)
                val repairedTool = tool.clone()
                val repairedMeta = repairedTool.itemMeta as Damageable
                repairedMeta.damage = 0
                repairedTool.itemMeta = repairedMeta

                val repairRecipe = MerchantRecipe(repairedTool, 999)
                repairRecipe.addIngredient(tool)
                repairRecipe.addIngredient(repairItems)
                trades.add(repairRecipe)
            }
        }

        // Trades de encantamentos customizados
        addCustomEnchantmentTrades(tool, trades)

        if (trades.isEmpty()) {
            player.sendMessage("§4[SSG] §2Nenhuma melhoria disponível para este item")
            return
        }

        val title = ItemEvolutionPlugin.instance.config.getString("merchant-title") ?: "Mercador"
        val merchant = Bukkit.createMerchant(Component.text(title))
        merchant.recipes = trades

        // Salvar o item original nos metadados do jogador
        //val meta = player.itemMeta ?: player.persistentDataContainer
        //meta.set(toolItemKey, PersistentDataType.STRING, serializeItemStack(tool))

        // Remover o item do inventário temporariamente
        //player.inventory.removeItem(tool)

        player.openMerchant(merchant, true)
        player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 2.0f)
    }

    private fun addCustomEnchantmentTrades(tool: ItemStack, trades: MutableList<MerchantRecipe>) {
        val config = ItemEvolutionPlugin.evolutionsConfig ?: return
        val toolType = ItemUtils.testTool(tool)

        if (!config.contains("bie.encantamentos")) return

        val enchantments = config.getConfigurationSection("bie.encantamentos")?.getKeys(false) ?: return

        for (enchantName in enchantments) {
            val enchantSection = config.getConfigurationSection("bie.encantamentos.$enchantName") ?: continue

            // Verificar se a ferramenta pode usar este encantamento
            if (!config.getBoolean("bie.ferramentas.$toolType.$enchantName", false)) continue

            val costPoints = enchantSection.getIntegerList("custopontos")
            val costItems = enchantSection.getStringList("custoitem")

            val currentLevel = getCurrentEnchantmentLevel(tool, enchantName)
            val nextLevel = currentLevel + 1

            if (nextLevel > costPoints.size) continue // Nível máximo atingido

            val requiredPoints = costPoints.getOrNull(nextLevel - 1) ?: continue
            val currentPoints = getCurrentPoints(tool)

            val rawCostString = costItems.getOrNull(nextLevel - 1) ?: continue
            val trimmed = rawCostString.trim()

            val qtyRegex = Regex("^([0-9]+)x?\\s+(.+)$")
            val match = qtyRegex.find(trimmed)

            val quantity = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val itemSpec = match?.groupValues?.get(2) ?: trimmed

            val cost: ItemStack = if (itemSpec.startsWith("nexo:")) {
                val itemId = itemSpec.removePrefix("nexo:").trim()
                try {
                    val itemBuilder = NexoItems.itemFromId(itemId)
                    if (itemBuilder == null) {
                        Bukkit.getLogger().warning("ID do Nexo inválido: $itemId")
                        continue
                    }
                    val itemStack = itemBuilder.build()
                    // Ajusta a quantidade, evitando exceder o max stack
                    itemStack.amount = quantity.coerceAtLeast(1).coerceAtMost(itemStack.maxStackSize)
                    itemStack
                } catch (e: Exception) {
                    Bukkit.getLogger().warning("Erro ao criar item Nexo ($itemId): ${e.message}")
                    continue
                }
            } else {
                // Delega para parseItemStack — ele já sabe tratar quantidades como "1 diamond"
                parseItemStack(rawCostString)
            }

            if (requiredPoints > currentPoints) {
                // Adiciona efeito de brilho
                cost.addUnsafeEnchantment(Enchantment.UNBREAKING, 10)

                // Define a lore avisando que os pontos são insuficientes
                val meta = cost.itemMeta
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

                meta?.lore = listOf("§cPontos insuficientes: $currentPoints/$requiredPoints")

                cost.itemMeta = meta
            }


            // ✅ Upgrade seguro com limite máximo
            val enchantedTool = EnchantmentHandler.upgradeEnchantment(tool, enchantName, maxLevel = costPoints.size)

            // ✅ Reduz pontos somente se realmente aplicou upgrade
            //if (requiredPoints <= currentPoints) {
            reducePoints(enchantedTool, requiredPoints)
            //}

            ItemUtils.updateLore(enchantedTool)

            val recipe = MerchantRecipe(enchantedTool, 999)
            recipe.addIngredient(tool)
            recipe.addIngredient(cost)
            trades.add(recipe)
        }
    }


    private fun getCurrentEnchantmentLevel(tool: ItemStack, enchantName: String): Int {
        val meta = tool.itemMeta ?: return 0
        val container = meta.persistentDataContainer
        val customEnchantmentsKey = NamespacedKey(ItemEvolutionPlugin.instance, "custom_enc")
        val customLevelsKey = NamespacedKey(ItemEvolutionPlugin.instance, "custom_enc_lvl")

        val enchantments = container.get(customEnchantmentsKey, PersistentDataType.STRING)?.split(",") ?: return 0
        val levels = container.get(customLevelsKey, PersistentDataType.STRING)?.split(",") ?: return 0

        val index = enchantments.indexOf(enchantName)
        return if (index >= 0 && index < levels.size) {
            levels[index].toIntOrNull() ?: 0
        } else 0
    }

    private fun getCurrentPoints(tool: ItemStack): Int {
        val meta = tool.itemMeta ?: return 0
        val container = meta.persistentDataContainer
        val pointsKey = NamespacedKey(ItemEvolutionPlugin.instance, "fplus_pontos")
        return container.get(pointsKey, PersistentDataType.INTEGER) ?: 0
    }

    private fun reducePoints(tool: ItemStack, points: Int) {
        val meta = tool.itemMeta ?: return
        val container = meta.persistentDataContainer
        val pointsKey = NamespacedKey(ItemEvolutionPlugin.instance, "fplus_pontos")

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
            val enchantName = parts[2].lowercase() // NamespacedKey usa lowercase
            val level = parts[3].toIntOrNull() ?: 1
            val meta = item.itemMeta as EnchantmentStorageMeta

            // Tenta obter o encantamento vanilla pelo NamespacedKey
            val key = NamespacedKey.minecraft(enchantName)
            val enchant = Enchantment.getByKey(key)

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