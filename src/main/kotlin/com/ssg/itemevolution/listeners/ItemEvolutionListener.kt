package com.ssg.itemevolution.listeners

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.ssg.itemevolution.enchantments.mining.AreaMiningEnchantment
import com.ssg.itemevolution.handlers.*
import com.ssg.itemevolution.services.EnchantmentService
import com.ssg.itemevolution.services.ScoreboardService
import com.ssg.itemevolution.services.SoulToolService
import com.ssg.itemevolution.ui.SoulToolDialog
import com.ssg.itemevolution.utils.ItemUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Snowman
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack


/**
 * Listener principal refatorado para usar o sistema de eventos centralizado
 */
class ItemEvolutionListener(
    private val soulToolDialog: SoulToolDialog,
    private val eventManager: EventManager,
    private val itemUtils: ItemUtils,
    private val enchantmentService: EnchantmentService,
    private val merchantHandler: MerchantHandler,
    private val soulToolService: SoulToolService,
    private val areaMiningEnchantment: AreaMiningEnchantment,
    private val scoreboardService: ScoreboardService
) : Listener {
    private val hasNexo = Bukkit.getPluginManager().getPlugin("Nexo") != null

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!hasNexo) return
        val clickedBlock = event.clickedBlock
        val player = event.player
        val tool = player.inventory.itemInMainHand
        if (isSmithingTable(clickedBlock)) {
            event.setUseItemInHand(Event.Result.DENY)
            event.setUseInteractedBlock(Event.Result.DENY)
            event.isCancelled = true
            if (soulToolService.hasSoul(tool)) {
                merchantHandler.openMerchant(player, tool)
            } else {
                soulToolDialog.checkEligibilityAndShowDialog(player, tool)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onNexoFurnitureInteract(event: NexoFurnitureInteractEvent) {
        val mechanic: FurnitureMechanic = event.mechanic
        val player = event.player
        val tool = event.itemInHand

        if (mechanic.itemID == "bancada_de_melhoria") {
            if (soulToolService.hasSoul(tool)) {
                merchantHandler.openMerchant(player, tool)
            } else {
                soulToolDialog.checkEligibilityAndShowDialog(player, tool)
            }
        }
    }

//    @EventHandler
//    fun onArmorChange(event: com.destroystokyo.paper.event.player.PlayerArmorChangeEvent) {
//        val player = event.player
//        player.sendMessage("§e--- DEBUG PlayerArmorChangeEvent ---")
//        player.sendMessage("§7Slot: §f${event.slot}")
//        player.sendMessage("§7SlotType (deprecated): §f${event.slotType}")
//        player.sendMessage("§7Old Item: §f${event.oldItem?.type ?: "null"} x${event.oldItem?.amount ?: 0}")
//        player.sendMessage("§7New Item: §f${event.newItem?.type ?: "null"} x${event.newItem?.amount ?: 0}")
//        player.sendMessage("§e--- END DEBUG ---")
//    }

//    @EventHandler
//    fun onPlayerInteract(event: PlayerInteractEvent) {
//        event.setUseItemInHand(Event.Result.DENY)
//        event.isCancelled = true
//        val player = event.player
//        player.sendMessage("§e--- DEBUG PlayerInteractEvent ---")
//        player.sendMessage("§7Action: §f${event.action}")
//        player.sendMessage("§7Item: §f${event.item?.type ?: "null"} x${event.item?.amount ?: 0}")
//        player.sendMessage("§7Hand: §f${event.hand}")
//        player.sendMessage("§7Clicked Block: §f${event.clickedBlock?.type ?: "null"}")
//        player.sendMessage("§7Block Face: §f${event.blockFace}")
//        player.sendMessage("§7Use Item In Hand: §f${event.useItemInHand()}")
//        player.sendMessage("§7Use Interacted Block: §f${event.useInteractedBlock()}")
//        player.sendMessage("§7Cancelled: §f${event.isCancelled}")
//        player.sendMessage("§e--- END DEBUG ---")
//    }



    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val tool = player.inventory.itemInMainHand
        val block = event.block

        // Processar encantamentos de mineração
        processEnchantments(player, tool, block, EnchantmentEventType.BLOCK_BREAK, event)

        // Processar evolução do item
        if (!event.isCancelled){
            processItemEvolution(player, tool, ItemUsageType.BLOCK_BREAK)
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val tool = attacker.inventory.itemInMainHand

        // Processar evolução do item
        processItemEvolution(attacker, tool, ItemUsageType.ATTACK)

        // Processar encantamentos de combate
        processEnchantments(attacker, tool, event.entity, EnchantmentEventType.ENTITY_DAMAGE, event)
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
            processArmorDamage(victim, event)
        }
    }

    @EventHandler
    fun onItemChange(event: PlayerItemHeldEvent) {
        val player = event.player
        val newItem = player.inventory.getItem(event.newSlot)
        val oldItem = player.inventory.getItem(event.previousSlot)

        areaMiningEnchantment.removeAreaMiningModifier(player, oldItem)
        areaMiningEnchantment.applyAreaMiningModifier(player, newItem)

        if (newItem != null && soulToolService.hasSoul(newItem)) {
            scoreboardService.showFor(player, newItem)
        } else {
            scoreboardService.hideFor(player)
        }
    }


    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        merchantHandler.restoreToolItem(player)
    }

    // MÉTODOS AUXILIARES

    private fun isSmithingTable(block: org.bukkit.block.Block?): Boolean {
        // Verificar se é uma mesa de ferreiro vanilla
        if (block == null) return false
        if (hasNexo) {
            return NexoFurniture.isFurniture(block.location) && NexoFurniture.furnitureMechanic(NexoFurniture.baseEntity(block.location))?.itemID == "bancada_de_melhoria"
        } else {
            if (block.type == Material.SMITHING_TABLE) return true
        }

        return false
    }

    private fun processItemEvolution(player: Player, tool: ItemStack, usageType: ItemUsageType) {
        if (!soulToolService.hasSoul(tool)) return

        val upgradedTool = itemUtils.upgradeItem(tool)

        // Disparar evento de uso do item
        val usageEvent = EventBuilder.itemUsage(usageType)
            .player(player)
            .item(upgradedTool)
            .build()

        eventManager.fireItemUsageEvent(usageEvent)
        scoreboardService.updateLines(player, upgradedTool)
    }

    private fun processEnchantments(
        player: Player,
        tool: ItemStack,
        target: Any,
        eventType: EnchantmentEventType,
        originalEvent: Any? = null
    ) {
        if (!soulToolService.hasSoul(tool)) return
        val enchantments = enchantmentService.listEnchantments(tool)

        // PASSO 1: Disparar o evento BEFORE_USE para todos os encantamentos
        // O EternaHandler já irá lidar com a verificação de item quebrado
        val preEventContext = mutableMapOf<String, Any>(
            "player" to player,
            "item" to tool,
            "target" to target
        ).also { context ->
            when (eventType) {
                EnchantmentEventType.BLOCK_BREAK -> context["breakEvent"] = originalEvent!!
                EnchantmentEventType.ENTITY_DAMAGE -> context["damageEvent"] = originalEvent!!
                else -> {}
            }
        }

        for ((enchantKey, level) in enchantments) {
            val beforeEvent = EventBuilder.enchantment(enchantKey, level, EnchantmentEventType.BEFORE_USE)
                .player(player)
                .item(tool)
                .apply { preEventContext.forEach { (key, value) -> context(key, value) } }
                .build()

            val beforeResult = eventManager.fireEnchantmentEvent(beforeEvent)

            if (beforeResult == EnchantmentEventResult.CANCELLED) {
                if (originalEvent is org.bukkit.event.Cancellable) {
                    originalEvent.isCancelled = true
                }
                return // Para TUDO
            }
        }

        // Se o evento não foi cancelado, procede para a lógica principal
        val mainEventContext = mutableMapOf<String, Any>(
            "player" to player,
            "item" to tool,
            "target" to target
        ).also { context ->
            when (eventType) {
                EnchantmentEventType.BLOCK_BREAK -> context["breakEvent"] = originalEvent!!
                EnchantmentEventType.ENTITY_DAMAGE -> context["damageEvent"] = originalEvent!!
                else -> {}
            }
        }

        // PASSO 2: Disparar o evento principal (BLOCK_BREAK ou ENTITY_DAMAGE)
        // Isso deve ser feito para todos os encantamentos aplicáveis.
        // O seu `EnchantmentEventHandlers` já contém a lógica para cada tipo.
        for ((enchantKey, level) in enchantments) {
            val mainEvent = EventBuilder.enchantment(enchantKey, level, eventType)
                .player(player)
                .item(tool)
                .apply { mainEventContext.forEach { (key, value) -> context(key, value) } }
                .build()

            eventManager.fireEnchantmentEvent(mainEvent)
        }

        // PASSO 3: Disparar o evento AFTER_USE para todos os encantamentos
        // O EternaHandler irá verificar a durabilidade após o evento principal
        val postEventContext = mutableMapOf<String, Any>(
            "player" to player,
            "item" to tool,
            "target" to target
        ).also { context ->
            when (eventType) {
                EnchantmentEventType.BLOCK_BREAK -> context["breakEvent"] = originalEvent!!
                EnchantmentEventType.ENTITY_DAMAGE -> context["damageEvent"] = originalEvent!!
                else -> {}
            }
        }

        for ((enchantKey, level) in enchantments) {
            val afterEvent = EventBuilder.enchantment(enchantKey, level, EnchantmentEventType.AFTER_USE)
                .player(player)
                .item(tool)
                .apply { postEventContext.forEach { (key, value) -> context(key, value) } }
                .build()

            eventManager.fireEnchantmentEvent(afterEvent)
        }
    }

    private fun processArmorDamage(player: Player, damageEvent: EntityDamageEvent) {
        val armorPieces = listOf(
            player.inventory.helmet,
            player.inventory.chestplate,
            player.inventory.leggings,
            player.inventory.boots
        )

        armorPieces.filterNotNull().forEach { armor ->
            if (itemUtils.isValidTool(armor) && soulToolService.hasSoul(armor)) {
                val upgradedArmor = itemUtils.upgradeItem(armor)
                armor.itemMeta = upgradedArmor.itemMeta

                // Processar encantamentos de defesa
                processEnchantments(player, armor, damageEvent, EnchantmentEventType.ENTITY_DAMAGED, damageEvent)
            }
        }
    }
}

/**
 * Listener separado para interações com o merchant
 */
class MerchantInteractionListener(
    private val merchantHandler: MerchantHandler
) : Listener {
    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        merchantHandler.restoreToolItem(player)
    }
}