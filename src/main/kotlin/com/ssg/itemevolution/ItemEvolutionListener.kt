package com.ssg.itemevolution

import com.nexomc.nexo.api.NexoFurniture
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.entity.Snowman
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.NamespacedKey
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.util.Vector
import kotlin.math.abs

class ItemEvolutionListener : Listener {

    private val breakSaveKey = NamespacedKey(ItemEvolutionPlugin.instance, "break_save")
    private val heldArmorIntercept: MutableMap<java.util.UUID, ItemStack> = mutableMapOf()
    private val hasNexo = Bukkit.getPluginManager().getPlugin("Nexo") != null

    fun interceptToolAndOpenMerchant(player: Player, tool: ItemStack){
        if (ItemUtils.testTool(tool).isNotEmpty()) {
            heldArmorIntercept[player.uniqueId] = tool.clone() // Clona para evitar modificações
            player.inventory.removeItem(tool) // Remove o item da mão do jogador

            // Abrir o mercador num próximo tick para evitar conflitos de eventos.
            // Passaremos o item interceptado para o mercador.
            Bukkit.getScheduler().runTaskLater(ItemEvolutionPlugin.instance, Runnable {
                val interceptedTool = heldArmorIntercept.remove(player.uniqueId)

                if (interceptedTool != null) {
                    // Se a mão principal estiver vazia, coloca o item lá
                    if (player.inventory.itemInMainHand.isEmpty) {
                        player.inventory.setItemInMainHand(interceptedTool)
                    } else {
                        // Senão, adiciona no inventário
                        player.inventory.addItem(interceptedTool)
                    }

                    MerchantHandler.openMerchant(player, interceptedTool)
                } else {
                    // Se por algum motivo o item não foi interceptado,
                    // usamos o item atual na mão
                    MerchantHandler.openMerchant(player, player.inventory.itemInMainHand)
                }

                player.updateInventory() // Garante que o cliente veja a mudança
            }, 1L)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!hasNexo) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return
        if (NexoFurniture.isFurniture(clickedBlock.location)) {
            val player = event.player
            val baseEntity = NexoFurniture.baseEntity(clickedBlock.location)
            val furnitureMechanic = NexoFurniture.furnitureMechanic(baseEntity)

            if (furnitureMechanic?.itemID == "bancada_de_melhoria") {
                event.isCancelled = true
                event.setUseItemInHand(Event.Result.DENY)
                event.setUseInteractedBlock(Event.Result.DENY)

                val tool = player.inventory.itemInMainHand
                interceptToolAndOpenMerchant(player, tool)
            }
        }
    }


    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val tool = attacker.inventory.itemInMainHand

        val toolCategory = ItemUtils.testTool(tool)

        when (toolCategory) {
            "bow", "crossbow" -> {
                attacker.inventory.setItemInMainHand(ItemUtils.upgradeItem(tool))
            }
            "sword", "axe" -> {
                val upgradedTool = ItemUtils.upgradeItem(tool)
                attacker.inventory.setItemInMainHand(upgradedTool)

                // Aplicar dano ao item (simulando desgaste)
                damageItem(upgradedTool, attacker)
            }
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return
        val attacker = (event as? EntityDamageByEntityEvent)?.damager

        // Não processar dano de boneco de neve
        if (attacker is Snowman) return

        if (event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
            event.cause == EntityDamageEvent.DamageCause.PROJECTILE) {

            // Processar armaduras
            processArmorUpgrade(victim.inventory.helmet)
            processArmorUpgrade(victim.inventory.chestplate)
            processArmorUpgrade(victim.inventory.leggings)
            processArmorUpgrade(victim.inventory.boots)
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val tool = player.inventory.itemInMainHand
        val toolCategory = ItemUtils.testTool(tool)

        if (toolCategory in listOf("axe", "hoe", "pickaxe", "shovel")) {
            val upgradedTool = ItemUtils.upgradeItem(tool)
            player.inventory.setItemInMainHand(upgradedTool)

            // Processar encantamentos customizados
            processCustomEnchantments(player, upgradedTool, event)

            // Salvar direção do break para o Amplificador
            saveBreakDirection(player, event)
        }
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return

        // Restaurar item de ferramenta quando abrir inventário do mercador
        MerchantHandler.restoreToolItem(player)
    }

    @EventHandler
    fun onEnchantItem(event: EnchantItemEvent) {
        val item = event.item
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        val encKey = NamespacedKey(ItemEvolutionPlugin.instance, "fplus_enc")

        val encValue = container.get(encKey, PersistentDataType.INTEGER) ?: 1

        if (encValue == 0) {
            event.isCancelled = true
            return
        }

        // Marcar item como encantado
        container.set(encKey, PersistentDataType.INTEGER, 1)
        item.itemMeta = meta
        ItemUtils.updateLore(item)
    }

    private fun processArmorUpgrade(armor: ItemStack?) {
        if (armor != null && ToolType.getAllToolMaterials().contains(armor.type)) {
            val upgradedArmor = ItemUtils.upgradeItem(armor)
            armor.itemMeta = upgradedArmor.itemMeta
            armor.addEnchantments(upgradedArmor.enchantments)
        }
    }

    private fun damageItem(item: ItemStack, player: Player) {
        if (item.type.maxDurability > 0) {
            val meta = item.itemMeta as? org.bukkit.inventory.meta.Damageable ?: return
            val newDamage = meta.damage + 1

            if (newDamage >= item.type.maxDurability) {
                // Item quebrou
                player.inventory.setItemInMainHand(ItemStack(Material.AIR))
                player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
            } else {
                meta.damage = newDamage
                item.itemMeta = meta
            }
        }
    }

    private fun processCustomEnchantments(player: Player, tool: ItemStack, event: BlockBreakEvent) {
        val meta = tool.itemMeta ?: return
        val container = meta.persistentDataContainer

        val customEnchantmentsKey = NamespacedKey(ItemEvolutionPlugin.instance, "custom_enc")
        val customLevelsKey = NamespacedKey(ItemEvolutionPlugin.instance, "custom_enc_lvl")

        val enchantments = container.get(customEnchantmentsKey, PersistentDataType.STRING)?.split(",") ?: return
        val levels = container.get(customLevelsKey, PersistentDataType.STRING)?.split(",") ?: return

        for (i in enchantments.indices) {
            if (i >= levels.size) continue

            val enchantName = enchantments[i]
            val level = levels[i].toIntOrNull() ?: 1

            when (enchantName) {
                "Amplificador" -> processAmplifierEnchantment(player, level, event)
            }
        }
    }

    private fun processAmplifierEnchantment(player: Player, level: Int, event: BlockBreakEvent) {
        val block = event.block
        val breakSpeed = getBlockBreakSpeed(block, player)
        val slownessDuration = (1.0 / breakSpeed / 2.0 * 20).toLong() // Converter para ticks

        // Aplicar Mining Fatigue
        val fatigueLevel = if (level <= 2) 0 else 1
        player.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, slownessDuration.toInt(), fatigueLevel, false, false))

        // Quebrar blocos adicionais
        val direction = getBreakDirection(player)
        val vectors = getAmplifierVectors(direction, level, player.location.yaw)

        for (vector in vectors) {
            val targetBlock = block.location.add(vector).block
            if (getBlockBreakSpeed(targetBlock, player) >=  breakSpeed && targetBlock.type != Material.BEDROCK) {
                targetBlock.breakNaturally(player.inventory.itemInMainHand)
            }
        }
    }

    private fun getAmplifierVectors(direction: BlockFace, level: Int, yaw: Float): List<Vector> {
        val vectors = mutableListOf<Vector>()

        when (direction) {
            BlockFace.NORTH, BlockFace.SOUTH -> {
                vectors.add(Vector(0, 1, 0))
                vectors.add(Vector(0, -1, 0))
                if (level >= 2) {
                    vectors.add(Vector(1, 0, 0))
                    vectors.add(Vector(-1, 0, 0))
                    if (level == 3) {
                        vectors.add(Vector(-1, -1, 0))
                        vectors.add(Vector(-1, 1, 0))
                        vectors.add(Vector(1, -1, 0))
                        vectors.add(Vector(1, 1, 0))
                    }
                }
            }
            BlockFace.WEST, BlockFace.EAST -> {
                vectors.add(Vector(0, 1, 0))
                vectors.add(Vector(0, -1, 0))
                if (level >= 2) {
                    vectors.add(Vector(0, 0, 1))
                    vectors.add(Vector(0, 0, -1))
                    if (level == 3) {
                        vectors.add(Vector(0, -1, -1))
                        vectors.add(Vector(0, 1, -1))
                        vectors.add(Vector(0, -1, 1))
                        vectors.add(Vector(0, 1, 1))
                    }
                }
            }
            BlockFace.UP, BlockFace.DOWN -> {
                if (level == 1) {
                    val yawRounded = (yaw / 90).toInt()
                    if (yawRounded == 1 || yawRounded == 3) {
                        vectors.add(Vector(1, 0, 0))
                        vectors.add(Vector(-1, 0, 0))
                    } else {
                        vectors.add(Vector(0, 0, -1))
                        vectors.add(Vector(0, 0, 1))
                    }
                } else if (level >= 2) {
                    vectors.add(Vector(1, 0, 0))
                    vectors.add(Vector(-1, 0, 0))
                    vectors.add(Vector(0, 0, -1))
                    vectors.add(Vector(0, 0, 1))
                }

                if (level == 3) {
                    vectors.add(Vector(-1, 0, -1))
                    vectors.add(Vector(-1, 0, 1))
                    vectors.add(Vector(1, 0, -1))
                    vectors.add(Vector(1, 0, 1))
                }
            }
            else -> {}
        }

        return vectors
    }

    private fun saveBreakDirection(player: Player, event: BlockBreakEvent) {
        val playerLoc = player.location
        val blockLoc = event.block.location

        val direction = when {
            abs(playerLoc.x - blockLoc.x) > abs(playerLoc.z - blockLoc.z) -> {
                if (playerLoc.x > blockLoc.x) BlockFace.WEST else BlockFace.EAST
            }
            abs(playerLoc.y - blockLoc.y) > abs(playerLoc.x - blockLoc.x) && abs(playerLoc.y - blockLoc.y) > abs(playerLoc.z - blockLoc.z) -> {
                if (playerLoc.y > blockLoc.y) BlockFace.DOWN else BlockFace.UP
            }
            else -> {
                if (playerLoc.z > blockLoc.z) BlockFace.NORTH else BlockFace.SOUTH
            }
        }

        player.persistentDataContainer.set(breakSaveKey, PersistentDataType.STRING, direction.name)
    }

    private fun getBreakDirection(player: Player): BlockFace {
        val directionName = player.persistentDataContainer.get(breakSaveKey, PersistentDataType.STRING)
        return if (directionName != null) {
            try {
                BlockFace.valueOf(directionName)
            } catch (e: IllegalArgumentException) {
                BlockFace.NORTH
            }
        } else {
            BlockFace.NORTH
        }
    }

    private fun getBlockBreakSpeed(block: org.bukkit.block.Block, player: Player): Double {
        // Implementação simplificada para obter velocidade de quebra do bloco
        // Em um plugin real, você precisaria calcular isso baseado no material, ferramenta e encantamentos
        return when (block.type) {
            Material.STONE, Material.COBBLESTONE -> 1.5
            Material.DIRT, Material.SAND -> 0.5
            Material.OBSIDIAN -> 50.0
            else -> 1.0
        }
    }
}